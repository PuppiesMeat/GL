
varying vec2 v_TexCoord;
attribute vec2 vCoordinate;
attribute vec2 a_Position;

void main(){
    gl_Position = u_Matrix * a_Position;
    v_TexCoord = vCoordinate;
}