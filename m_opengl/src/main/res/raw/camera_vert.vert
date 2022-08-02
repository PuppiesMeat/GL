//顶点着色器
attribute vec4 vPosition;//顶点坐标
attribute vec4 vCoord;//纹理坐标
uniform mat4 vMatrix;
//传给片元着色器 像素点
varying vec2 aCoord;
void main(){
    gl_Position = vPosition;
    aCoord= (vMatrix * vCoord).xy;

}