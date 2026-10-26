"""Generate native body clocks and vanilla timelines stretched to production solar days."""
from __future__ import annotations

import argparse
import json
import math
from pathlib import Path
import re
import struct
import zipfile


def float32(value: float) -> float:
    return struct.unpack('f', struct.pack('f', value))[0]


def body_periods(solar_source: str, sky_source: str) -> dict[str, int]:
    """Evaluate the literal body tree using SolarSystem.Body's Java float/double operations."""
    declarations = {}
    for match in re.finditer(r'new Body\("([^"\n]+)", ([^\n]+)', solar_source):
        name = match.group(1)
        fields = match.group(2).split(', ')
        end = match.start()
        depth = 0
        for end in range(solar_source.index('(', match.start()), len(solar_source)):
            char = solar_source[end]
            if char == '(':
                depth += 1
            elif char == ')':
                depth -= 1
                if depth == 0:
                    break
        if name in declarations:
            raise ValueError(f'Duplicate body declaration {name}')
        declarations[name] = {
            'start': match.start(), 'end': end,
            'mass': float32(float(fields[0].removesuffix('F'))),
            'axis': float32(float(fields[2].removesuffix('F'))),
            'rotation': int(fields[7]),
        }
    for body in declarations.values():
        parents = [(other_name, other) for other_name, other in declarations.items()
                   if other['start'] < body['start'] < body['end'] < other['end']]
        body['parent'] = min(parents, key=lambda entry: entry[1]['end'] - entry[1]['start'])[0] if parents else None
    roots = [name for name, body in declarations.items() if body['parent'] is None]
    if roots != ['kerbol']:
        raise ValueError(f'Unexpected body roots: {roots}')

    registered = re.search(r'for \(var dimension : java\.util\.List\.of\((.*?)\)\) \{', sky_source, re.DOTALL)
    if registered is None:
        raise ValueError('Cannot find the registered body-clock dimensions')
    dimensions = [name.lower() for name in re.findall(r'SpaceDimensions\.([A-Z]+)', registered.group(1))]
    if len(set(dimensions)) != len(dimensions):
        raise ValueError('A body-clock dimension is listed twice')
    gravity = float32(float(re.search(r'GRAVITATIONAL_CONSTANT = ([\d.e+-]+)F', solar_source).group(1)))
    periods = {}
    for dimension in dimensions:
        # CelestialBodies explicitly resolves Thatmo's level to Kerbin, matching the source provider.
        name = 'kerbin' if dimension == 'thatmo' else dimension
        body = declarations[name]
        planet = body
        while declarations[planet['parent']]['parent'] is not None:
            planet = declarations[planet['parent']]
        parent = declarations[planet['parent']]
        axis = float32(planet['axis'] * 1000)
        year = 2 * math.pi * math.sqrt(axis * axis * axis / (gravity * parent['mass'])) / 21600
        day = body['rotation'] * (1200 / 21600) * 20 / (1 - 1 / year)
        periods[dimension] = math.floor(day + .5)
    return periods


def resources(solar: Path, skies: Path, minecraft: Path, authored: Path) -> dict[str, str]:
    periods = body_periods(solar.read_text(encoding='utf-8'), skies.read_text(encoding='utf-8'))
    generated = {}
    with zipfile.ZipFile(minecraft) as archive:
        for body, period in periods.items():
            dimension = json.loads((authored / f'data/ntmspace/dimension_type/{body}.json').read_text(encoding='utf-8'))
            dimension['default_clock'] = f'ntmspace:{body}'
            dimension['timelines'] = [f'ntmspace:{body}_day', f'ntmspace:{body}_villager_schedule']
            generated[f'data/ntmspace/dimension_type/{body}.json'] = json.dumps(dimension, indent=2) + '\n'
            generated[f'data/ntmspace/world_clock/{body}.json'] = '{}\n'
            for source, suffix in [('day', 'day'), ('villager_schedule', 'villager_schedule')]:
                timeline = json.loads(archive.read(f'data/minecraft/timeline/{source}.json'))
                previous_period = timeline['period_ticks']

                def scale(ticks):
                    return min(period - 1, math.floor(ticks * period / previous_period + .5))

                timeline['clock'] = f'ntmspace:{body}'
                timeline['period_ticks'] = period
                for key, value in timeline.get('time_markers', {}).items():
                    if isinstance(value, dict):
                        value['ticks'] = scale(value['ticks'])
                    else:
                        timeline['time_markers'][key] = scale(value)
                for track in timeline.get('tracks', {}).values():
                    for frame in track['keyframes']:
                        frame['ticks'] = scale(frame['ticks'])
                generated[f'data/ntmspace/timeline/{body}_{suffix}.json'] = json.dumps(timeline, indent=2) + '\n'
    return generated


def main():
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--minecraft-jar', required=True, type=Path)
    parser.add_argument('--solar-source', type=Path, default=root / 'src/main/java/com/ntmspace/world/SolarSystem.java')
    parser.add_argument('--skies-source', type=Path, default=root / 'src/main/java/com/ntmspace/world/SpaceSkies.java')
    parser.add_argument('--output', type=Path, default=root / 'src/main/resources')
    parser.add_argument('--authored-resources', type=Path, default=root / 'src/main/resources')
    parser.add_argument('--check', action='store_true', help='Compare existing resources without modifying them')
    args = parser.parse_args()
    generated = resources(args.solar_source, args.skies_source, args.minecraft_jar, args.authored_resources)
    mismatches = []
    for relative, text in generated.items():
        path = args.output / relative
        if args.check:
            if not path.exists() or path.read_text(encoding='utf-8') != text:
                mismatches.append(relative)
        else:
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(text, encoding='utf-8')
    if mismatches:
        raise SystemExit('Generated body-clock resources differ: ' + ', '.join(mismatches))
    print(f'{"Checked" if args.check else "Generated"} {len(generated)} native clock/timeline resources')


if __name__ == '__main__':
    main()
