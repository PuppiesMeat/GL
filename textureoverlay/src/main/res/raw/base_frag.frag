precision mediump float;
varying vec2 v_TexCoord;
uniform sampler2D u_TextureUnit1;
uniform sampler2D u_TextureUnit2;
void main() {
    vec4 texture1 = texture2D(u_TextureUnit1, v_TexCoord);
    vec4 texture2 = texture2D(u_TextureUnit2, v_TexCoord);
    if (texture1.a != 0.0) {
        gl_FragColor = texture1;
    } else {
        gl_FragColor = texture2;
    }
}