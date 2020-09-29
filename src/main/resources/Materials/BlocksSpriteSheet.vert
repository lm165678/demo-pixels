#import "Common/ShaderLib/GLSLCompat.glsllib"
#import "Common/ShaderLib/Skinning.glsllib"
#import "Common/ShaderLib/Instancing.glsllib"
#import "Common/ShaderLib/MorphAnim.glsllib"

attribute vec3 inPosition;
attribute vec2 inTexCoord;
attribute vec3 inTexCoord2;

varying vec2 texCoord;
varying vec3 voxelData;
// varying vec3 worldCoord;

void main(){

    vec4 modelSpacePos = vec4(inPosition, 1.0);

    texCoord = inTexCoord;
    voxelData = inTexCoord2;
    // worldCoord = (g_WorldMatrix * modelSpacePos).xyz;
    gl_Position = TransformWorldViewProjection(modelSpacePos);;

}