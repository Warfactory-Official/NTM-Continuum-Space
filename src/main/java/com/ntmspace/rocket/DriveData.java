package com.ntmspace.rocket;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;

/**
 * A navigation drive's NBT in 1.7 ({@code ItemVOTVdrive}): the landing coordinates {@code x}/{@code z} (for the orbit
 * drive, a station's grid square), {@code Processed}, {@code copied}, the approximate landing area the Stardar drew
 * ({@code ax}/{@code az}, rolled on first look), and what the server last told the stack about its station
 * ({@code stationName}, {@code sDim} (here the body's name), {@code sHas}). Saved by name, never a dimension number.
 */
public record DriveData(int x, int z, boolean processed, boolean copied, Optional<Integer> ax, Optional<Integer> az,
                        String stationName, String stationBody, boolean stationHas) {

    public static final DriveData EMPTY = new DriveData(0, 0, false, false, Optional.empty(), Optional.empty(), "", "", false);

    public static final Codec<DriveData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("x", 0).forGetter(DriveData::x),
            Codec.INT.optionalFieldOf("z", 0).forGetter(DriveData::z),
            Codec.BOOL.optionalFieldOf("processed", false).forGetter(DriveData::processed),
            Codec.BOOL.optionalFieldOf("copied", false).forGetter(DriveData::copied),
            Codec.INT.optionalFieldOf("ax").forGetter(DriveData::ax),
            Codec.INT.optionalFieldOf("az").forGetter(DriveData::az),
            Codec.STRING.optionalFieldOf("station_name", "").forGetter(DriveData::stationName),
            Codec.STRING.optionalFieldOf("station_body", "").forGetter(DriveData::stationBody),
            Codec.BOOL.optionalFieldOf("station_launched", false).forGetter(DriveData::stationHas)).apply(i, DriveData::new));

    public DriveData withCoordinates(int x, int z) {
        return new DriveData(x, z, processed, copied, ax, az, stationName, stationBody, stationHas);
    }

    public DriveData withProcessed(boolean processed) {
        return new DriveData(x, z, processed, copied, ax, az, stationName, stationBody, stationHas);
    }

    public DriveData withCopied(boolean copied) {
        return new DriveData(x, z, processed, copied, ax, az, stationName, stationBody, stationHas);
    }

    public DriveData withApproximate(int ax, int az) {
        return new DriveData(x, z, processed, copied, Optional.of(ax), Optional.of(az), stationName, stationBody, stationHas);
    }

    public DriveData withStation(String name, String body, boolean has) {
        return new DriveData(x, z, processed, copied, ax, az, name, body, has);
    }
}
