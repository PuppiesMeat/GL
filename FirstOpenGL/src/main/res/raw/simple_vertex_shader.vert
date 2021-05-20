/**
* 使用GLSL语法定义，GLSL是OpenGL的着色语言；这个着色语言的语法结构与C语言相似。
*/
attribute vec4 a_Position;
attribute vec4 a_Color;
uniform mat4 u_Matrix;

//varying是一个特殊的变量，它把给它的那些值进行混合，并把混合后的值发送给片段着色器
varying vec4 v_Color;

void main(){
    v_Color = a_Color;
    //    gl_Position = a_Position;
    gl_Position = u_Matrix * a_Position;
    gl_PointSize = 50.0;
}
