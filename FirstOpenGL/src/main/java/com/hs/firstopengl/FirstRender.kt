package com.hs.firstopengl

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.GLUtils.*
import android.opengl.Matrix.*
import android.opengl.GLSurfaceView
import java.lang.RuntimeException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FirstRender : GLSurfaceView.Renderer {

    val context: Context
    val fragShader: String?
    val vertexShader: String?
    var vertexObjectId = 0
    var fragObjectId = 0
    var programObjectId = 0

    //    val U_COLOR = "u_Color"
    var uColorLocation = 0
    val A_POSITION = "a_Position"
    var aPositionLocation = 0
    val A_COLOR = "a_Color"
    val U_MATRIX = "u_Matrix"
    val POSITION_COMPONENT_COUNT = 4
    val COLOR_COMPONENT_COUNT = 3
    private val BYTES_PER_FLOAT = 4
    val STRIDE = (POSITION_COMPONENT_COUNT + COLOR_COMPONENT_COUNT) * BYTES_PER_FLOAT
    var aColorLocation = 0
    val projectionMatrix = FloatArray(16)
    var uMatrixLocation = 0

    constructor(context: Context) {
        this.context = context
        /**
         * 这块内存是不受虚拟机垃圾回收器管理的， 当进程结束后，这块内存会被释放掉，因此一般不用关心它
         */
        vertexBuffer = ByteBuffer.allocateDirect(tableVerticesWithTriangles.size * BYTES_PER_FLOAT)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        /**
         * 把数据从Art、Dalvik内存复制到本地内存
         */
        vertexBuffer.put(tableVerticesWithTriangles)

        vertexShader = OpenGLUtil.readRawShader(context, R.raw.simple_vertex_shader)
        fragShader = OpenGLUtil.readRawShader(context, R.raw.simple_frag_shader)
        if (null == vertexShader || null == fragShader) throw RuntimeException("load shader code failed")


    }

    /**
     * 定义三角形顶点时，总是以逆时针的顺序排列顶点，这称为卷曲顺序（winding order），因为在
     * 任何地方都是用这种一直的卷曲顺序，可以优化性能。使用
     */
//    private val tableVertices = arrayOf(0f, 0f, 0f, 14f, 9f, 14f, 9f, 0f)
//    private val tableVerticesWithTriangles = floatArrayOf(
//        //triangle1
//        -0.5f, -0.5f,
//        0.5f, 0.5f,
//        -0.5f, 0.5f,
//        //triangle2
//        -0.5f, -0.5f,
//        0.5f, -0.5f,
//        0.5f, 0.5f,
//        //line 1
//        -0.5f, 0f,
//        0.5f, 0f,
//        //mallets
//        0f, -0.25f,
//        0f, 0.25f
//    )

    /**
     * 将顶点左边修改为GL_TRIANGLE_FAN
     */
//    private val tableVerticesWithTriangles = floatArrayOf(
//        //triangle fan
//        0f, 0f,
//        -0.5f, -0.5f,
//        0.5f, -0.5f,
//        0.5f, 0.5f,
//        -0.5f, 0.5f,
//        -0.5f, -0.5f,
//        //line 1
//        -0.5f, 0f,
//        0.5f, 0f,
//        //mallets
//        0f, -0.25f,
//        0f, 0.25f
//    )

    /**
     * 在GL_TRIANGLE_FAN顶点的情况下再为每个点增加顶点颜色值RGB
     */
    private val tableVerticesWithTriangles = floatArrayOf(
        0f, 0f, 1f, 1f, 1f,
        -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
        0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
        0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
        -0.5f, 0.8f, 0.7f, 0.7f, 0.7f,
        -0.5f, -0.8f, 0.7f, 0.7f, 0.7f,
        //line 1
        -0.5f, 0f, 1f, 0f, 0f,
        0.5f, 0f, 1f, 0f, 0f,
        //mallets
        0f, -0.25f, 0f, 0f, 1f,
        0f, 0.25f, 1f, 1f, 0f
    )


    /**
     * 要与本地系统交互比如OpenGL，需要用到本地堆
     */
    var vertexBuffer: FloatBuffer


    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        glClearColor(0f, 0f, 0f, 0f)


        vertexObjectId = OpenGLUtil.compileVertexShader(vertexShader!!)
        fragObjectId = OpenGLUtil.compileFragShader(fragShader!!)
        if (0 == vertexObjectId || 0 == fragObjectId) throw RuntimeException("create shader object failed")
        programObjectId = OpenGLUtil.linkProgram(vertexObjectId, fragObjectId)
        if (0 == programObjectId) throw RuntimeException("link program object failed")

        /**
         * link之后就可以去查询着色器中定义的变量位置了，一个uniform的位置在一个程序对象中是唯一的，即使在两个不同的
         * 程序中使用了相同的uniform名字，也不意味着他们使用了相同的位置。
         */
//        uColorLocation = glGetUniformLocation(programObjectId, U_COLOR)
        aColorLocation = glGetAttribLocation(programObjectId, A_COLOR)

        /**
         * 获取属性的位置，我们可以让OpenGL自动分配属性的位置编号，也可以在着色器被链接到一起之前，可以通过调用
         * glBindAttribLocation()由我们自己给它们分配位置编号。
         */
        aPositionLocation = glGetAttribLocation(programObjectId, A_POSITION)

        uMatrixLocation = glGetUniformLocation(programObjectId, U_MATRIX)

        /**
         * 关联属性与顶点数据的数组
         */
        //确保OpenGL从Buffer的开头读取数据
        vertexBuffer.position(0)
        //通知OpenGL它可以在vertexBuffer中找到顶点数据
        /**
         * int index 这个是属性的位置
         * int size 这是每个属性数据的计数，或者对于这个属性，有多少个分量与每一个顶点关联。这里我们每个顶点只传递
         *      2个分量，由于在着色器中我们定义了4个分量，OpenGL默认为前三个分量赋值为0，最后一个分量赋值为1
         * int type 这是数据的类型，我们把数据定义为浮点数据列表，传递GL_FLOAT
         * boolean normalized 只有使用整形数据的时候，这个参数才有意义，因此暂时忽略掉
         * int stride 只有当一个数组存储多余一个属性时才有意义，表示从数据第几个索引开始读取
         * Buffer ptr 这个参数告诉OpenGL从哪里读取数据，这里传入已经重新初始化position后的vertexBuffer
         */
        glVertexAttribPointer(
            aPositionLocation,
            POSITION_COMPONENT_COUNT,
            GL_FLOAT,
            false,
            STRIDE,
            vertexBuffer
        )

        /**
         * 使能顶点数据，尽管已经把数据属性链接起来了，在开始绘制之前，我们还需要调用glEnableVertexAttribArray
         * 使能这个属性，
         */
        glEnableVertexAttribArray(aPositionLocation)

        /**
         * 关联颜色与颜色数据的数组
         */
        vertexBuffer.position(POSITION_COMPONENT_COUNT)
        glVertexAttribPointer(
            aColorLocation,
            COLOR_COMPONENT_COUNT,
            GL_FLOAT,
            false,
            STRIDE,
            vertexBuffer
        )
        glEnableVertexAttribArray(aColorLocation)
        // 在onSurfaceCreated最后调用glUseProgram开始使用程序 告知OpenGL后续所有绘制都使用该程序
        glUseProgram(programObjectId)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        glViewport(0, 0, width, height)

        val aspectRation: Float =
            if (width > height) width.toFloat() / height.toFloat()
            else height.toFloat() / width.toFloat()
        if (width > height) {
            //landscape 如果在横屏模式下，我们会扩展宽度的坐标空间，这样它的取值范围就不是[-1,1]，而是从[-aspectRation,aspectRation]
            //同理竖屏也一样
            orthoM(projectionMatrix, 0, -aspectRation, aspectRation, -1f, 1f, -1f, 1f)
        } else {
            orthoM(projectionMatrix, 0, -1f, 1f, -aspectRation, aspectRation, -1f, 1f)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        glClear(GL_COLOR_BUFFER_BIT)

        /**
         * 给着色器传递那个正交投影矩阵
         */
        glUniformMatrix4fv(uMatrixLocation, 1, false, projectionMatrix, 0)

        /**
         * 更新着色器代码中的u_Color的值。与属性不同，uniform的分量没有默认值，因此，如果一个uniform在着色器中被定义为
         * vec4类型，必须提供所有4个分量的值。指定颜色后就可以开始绘制了
         */
//        glUniform4f(uColorLocation, 1.0f, 1.0f, 1.0f, 1.0f)

        /**
         * 开始绘制，第一个参数告诉OpenGL我们要画三角形。第二个参数是告诉OpenGL从定点数组的第几个索引开始读顶点，
         * 第三个参数告诉OpenGL读入6个顶点。
         */
        glDrawArrays(GL_TRIANGLE_FAN, 0, 6)

        /**
         * 开始绘制分割线
         */
//        glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f)
        glDrawArrays(GL_LINES, 6, 2)

        /**
         * 开始绘制木槌，但是OpenGL需要我们指定屏幕上的点的大小，因此需要在顶点着色器gl_Position赋值后，再设置下gl_PointSize的
         * 大小，当OpenGL把一个点分解成片段的时候，它会生成一些片段，它们是以gl_Position为中心的四边形，这些四边形的每条边的长度
         * 与gl_PointSize相等。gl_PointSize越大绘制出来的点越大。
         */
//        glUniform4f(uColorLocation, 0.0f, 0.0f, 1.0f, 1.0f);
        glDrawArrays(GL_POINTS, 8, 1);
//        glUniform4f(uColorLocation, 1.0f, 0.0f, 0.0f, 1.0f)
        glDrawArrays(GL_POINTS, 9, 1)
    }

    public fun release() {
        if (0 != programObjectId) glDeleteProgram(programObjectId)
        if (0 != vertexObjectId) glDeleteShader(vertexObjectId)
        if (0 != fragObjectId) glDeleteShader(fragObjectId)
    }

}