#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2DArray m_DirtTextures;

varying vec2 texCoord;
varying vec3 voxelData;
// varying vec3 worldCoord;

void main(){

    vec4 color = texture2DArray(m_DirtTextures, vec3(texCoord, voxelData.x));

    // multiply the color by the sunlight value.
    // for torches we want the max(sunlight, torch) so the brightest value "wins".
    color.rgb *= voxelData.z;

    gl_FragColor = color;
}