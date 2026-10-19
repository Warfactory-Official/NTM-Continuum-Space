#version 150

// NTM: Space (new-hbm-space): hbm-space's shaders/crescent.frag as the StarDar screen uses it (GUIMachineStardar: bodyTex
// on unit 0, useBodyAlphaMask 1), ported verbatim to Golem GuiCanvas.shaderQuads (GLSL 1.50, every node). Mechanical
// changes only: its uniforms are the call's parameters (PARAM, layout in com.ntmspace.client.PlanetShaders: 0 phase,
// 1 offset, 20 useBodyAlphaMask), its sampler Sampler0, gl_TexCoord[0] is texCoord0, texture2D texture, gl_FragColor
// fragColor.

#ifdef GOLEM_SKY_UBO
layout(std140) uniform GolemSkyShader {
    mat4 GolemSkyLocal;
    vec4 GolemSkyParams[16];
};
#define PARAM(i) GolemSkyParams[(i) / 4][(i) % 4]
#else
uniform float GolemSkyParams[64];
#define PARAM(i) GolemSkyParams[i]
#endif

in vec2 texCoord0;
in vec3 localPos;

out vec4 fragColor;

#define phase PARAM(0)
#define offset PARAM(1)
uniform sampler2D Sampler0;
#define bodyTex Sampler0
#define useBodyAlphaMask int(PARAM(20) + 0.5)

#define PI 3.1415926538

vec2 quantize(vec2 inp, vec2 period) {
	return floor(inp / period) * period;
}

void main() {
	vec2 movingUV = texCoord0 + vec2(offset, 0);

	// hello im john planets with transparent pixels, sorry for being here
	float alphaMask = 1.0;
	if (useBodyAlphaMask != 0) {
		alphaMask = texture(bodyTex, movingUV).a;
		if (alphaMask <= 0.001) {
			fragColor = vec4(0.0);
			return;
		}
	}

	vec2 fragCoord = quantize(movingUV, vec2(0.0625, 0.0625)) - vec2(offset, 0);
	vec2 uv = (2.25 * fragCoord - 1.1);
	vec2 suv = (2.0 * fragCoord - 1.0);

	vec3 light = vec3(sin(phase * PI), 0.0, cos(phase * PI));

	vec3 n = vec3(uv, sqrt(1.0 - clamp(dot(uv, uv), 0.0, 1.0)));
	float brightness = dot(n, light);

	// when nearly new moon, ring glow
	brightness = max(brightness, (abs(phase) - 0.7) * clamp(dot(suv, suv), 0.0, 1.0));

	// become full square when nearing full illumination
	if (abs(phase) < 0.5) {
		if (phase < 0.0) {
			brightness = phase * 4.0 + 2.0 - uv.x;
		} else {
			brightness = -phase * 4.0 + 2.0 + uv.x;
		}
	}

	// minimum brightness
	brightness = max(brightness, 0.05);

	fragColor = vec4(0.0, 0.0, 0.0, (1.0 - brightness) * alphaMask);
}
