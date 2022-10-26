precision highp float;
precision highp int;

varying vec2 aCoord;
uniform sampler2D uTexture;
uniform float uWidth;
uniform float uHeight;
const vec3 COEF_Y = vec3( 0.299,  0.587,  0.114);
const vec3 COEF_U = vec3(-0.147, -0.289,  0.436);
const vec3 COEF_V = vec3( 0.615, -0.515, -0.100);
const float UV_DIVIDE_LINE = 2.0 / 3.0;
float cY(float x, float y){
    vec4 c=texture2D(uTexture, vec2(x, y));
    return c.r*0.2126+c.g*0.7152+c.b*0.0722;
}
vec4 cC(float x, float y, float dx, float dy){
    vec4 c0=texture2D(uTexture, vec2(x, y));
    vec4 c1=texture2D(uTexture, vec2(x+dx, y));
    vec4 c2=texture2D(uTexture, vec2(x, y+dy));
    vec4 c3=texture2D(uTexture, vec2(x+dx, y+dy));
    return (c0+c1+c2+c3)/4.;
}
float cU(float x, float y, float dx, float dy){
    vec4 c=cC(x, y, dx, dy);
    return -0.09991*c.r - 0.33609*c.g + 0.43600*c.b+0.5000;
}
float cV(float x, float y, float dx, float dy){
    vec4 c=cC(x, y, dx, dy);
    return 0.61500*c.r - 0.55861*c.g - 0.05639*c.b+0.5000;
}
vec2 cPos(float t, float shiftx, float gy){
    vec2 pos=vec2(floor(uWidth*aCoord.x), floor(uHeight*gy));
    return vec2(mod(pos.x*shiftx, uWidth), (pos.y*shiftx+floor(pos.x*shiftx/uWidth))*t);
}
vec4 calculateY(){
    vec2 pos=cPos(1., 4., aCoord.y);
    vec4 oColor=vec4(0);
    float textureYPos=pos.y/uHeight;
    oColor[0]=cY(pos.x/uWidth, textureYPos);
    oColor[1]=cY((pos.x+1.)/uWidth, textureYPos);
    oColor[2]=cY((pos.x+2.)/uWidth, textureYPos);
    oColor[3]=cY((pos.x+3.)/uWidth, textureYPos);
    return oColor;
}
vec4 calculateU(float gy, float dx, float dy){
    vec2 pos=cPos(2., 8., aCoord.y-gy);
    vec4 oColor=vec4(0);
    float textureYPos=pos.y/uHeight;
    oColor[0]= cU(pos.x/uWidth, textureYPos, dx, dy);
    oColor[1]= cU((pos.x+2.)/uWidth, textureYPos, dx, dy);
    oColor[2]= cU((pos.x+4.)/uWidth, textureYPos, dx, dy);
    oColor[3]= cU((pos.x+6.)/uWidth, textureYPos, dx, dy);
    return oColor;
}

vec4 calculateV(float gy, float dx, float dy){
    vec2 pos=cPos(2., 8., aCoord.y-gy);
    vec4 oColor=vec4(0);
    float textureYPos=pos.y/uHeight;
    oColor[0]=cV(pos.x/uWidth, textureYPos, dx, dy);
    oColor[1]=cV((pos.x+2.)/uWidth, textureYPos, dx, dy);
    oColor[2]=cV((pos.x+4.)/uWidth, textureYPos, dx, dy);
    oColor[3]=cV((pos.x+6.)/uWidth, textureYPos, dx, dy);
    return oColor;
}

vec4 calculateUV(float dx, float dy){
    vec2 pos=cPos(2., 4., aCoord.y-0.2500);
    vec4 oColor=vec4(0);
    float textureYPos=pos.y/uHeight;
    oColor[0]= cU(pos.x/uWidth, textureYPos, dx, dy);
    oColor[1]= cV(pos.x/uWidth, textureYPos, dx, dy);
    oColor[2]= cU((pos.x+2.)/uWidth, textureYPos, dx, dy);
    oColor[3]= cV((pos.x+2.)/uWidth, textureYPos, dx, dy);
    return oColor;
}
vec4 calculateVU(float dx, float dy){
    vec2 pos=cPos(2., 4., aCoord.y-0.2500);
    vec4 oColor=vec4(0);
    float textureYPos=pos.y/uHeight;
    oColor[0]= cV(pos.x/uWidth, textureYPos, dx, dy);
    oColor[1]= cU(pos.x/uWidth, textureYPos, dx, dy);
    oColor[2]= cV((pos.x+2.)/uWidth, textureYPos, dx, dy);
    oColor[3]= cU((pos.x+2.)/uWidth, textureYPos, dx, dy);
    return oColor;
}

void main() {
//    if (aCoord.y<= UV_DIVIDE_LINE){
//        gl_FragColor=calculateY();
//    } else {
//        gl_FragColor=calculateVU(1./uWidth, 1./uHeight);
//    }
    vec2 texelOffset = vec2(1./uWidth, 0.0);
    if(v_texCoord.y <= UV_DIVIDE_LINE) {
        //在纹理坐标 y < (2/3) 范围，需要完成一次对整个纹理的采样，
        //一次采样（加三次偏移采样）4 个 RGBA 像素（R,G,B,A）生成 1 个（Y0,Y1,Y2,Y3），整个范围采样结束时填充好 width*height 大小的缓冲区；

        vec2 texCoord = vec2(v_texCoord.x, v_texCoord.y * 3.0 / 2.0);
        vec4 color0 = texture(s_TextureMap, texCoord);
        vec4 color1 = texture(s_TextureMap, texCoord + texelOffset);
        vec4 color2 = texture(s_TextureMap, texCoord + texelOffset * 2.0);
        vec4 color3 = texture(s_TextureMap, texCoord + texelOffset * 3.0);

        float y0 = dot(color0.rgb, COEF_Y);
        float y1 = dot(color1.rgb, COEF_Y);
        float y2 = dot(color2.rgb, COEF_Y);
        float y3 = dot(color3.rgb, COEF_Y);
        gl_FragColor = vec4(y0, y1, y2, y3);
    }
    else {
        //当纹理坐标 y > (2/3) 范围，一次采样（加三次偏移采样）4 个 RGBA 像素（R,G,B,A）生成 1 个（V0,U0,V0,U1），
        //又因为 VU plane 缓冲区的高度为 height/2 ，VU plane 在垂直方向的采样是隔行进行，整个范围采样结束时填充好 width*height/2 大小的缓冲区。
        vec2 texCoord = vec2(v_texCoord.x, (v_texCoord.y - UV_DIVIDE_LINE) * 3.0);
        vec4 color0 = texture(s_TextureMap, texCoord);
        vec4 color1 = texture(s_TextureMap, texCoord + texelOffset);
        vec4 color2 = texture(s_TextureMap, texCoord + texelOffset * 2.0);
        vec4 color3 = texture(s_TextureMap, texCoord + texelOffset * 3.0);

        float v0 = dot(color0.rgb, COEF_V) + 0.5;
        float u0 = dot(color1.rgb, COEF_U) + 0.5;
        float v1 = dot(color2.rgb, COEF_V) + 0.5;
        float u1 = dot(color3.rgb, COEF_U) + 0.5;
        gl_FragColor = vec4(v0, u0, v1, u1);
    }
}