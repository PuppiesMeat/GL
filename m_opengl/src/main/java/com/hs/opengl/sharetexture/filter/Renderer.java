/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hs.opengl.sharetexture.filter;

/**
 * Renderer 渲染接口，渲染的四个接口应该在同一个GL线程中调用
 *
 * @author wuwang
 * @version v1.0 2017:10:31 11:40
 */
public interface Renderer {

    /**
     * 创建
     */
    void create();

    /**
     * 大小改变
     * @param width 宽度
     * @param height 高度
     */
    void sizeChanged(int width, int height);

    /**
     * 渲染
     * @param texture 输入纹理
     */
    void draw(int texture);

    /**
     * 销毁
     */
    void destroy();

}

