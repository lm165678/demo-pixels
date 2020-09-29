#import "Common/ShaderLib/GLSLCompat.glsllib"

uniform sampler2D m_DirtTexture;

varying vec2 texCoord;
varying vec3 voxelData;
// varying vec3 worldCoord;

void main(){

    // the ID corresponds to which texture we want, so we have to do a col/row index.
    vec4 color = texture2DArray(m_DirtTexture, texCoord);

    // multiply the color by the sunlight value.
    // for torches we want the max(sunlight, torch) so the brightest value "wins".
    color.rgb *= voxelData.z;

    gl_FragColor = color;
}