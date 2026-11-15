package com.example.ui.benchmark

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt

/**
 * Real OpenGL ES 3.0 shader stress viewport executing a multi-octave cel-shaded
 * terrain + Sheikah prism lighting shader at the user's configured resolution scale
 * and FP16 (mediump) vs highp precision path, reporting real per-frame durations.
 */
@Composable
fun MaliShaderBenchmarkView(
    isRunning: Boolean,
    resolutionScale: Float,
    useFp16Valhall: Boolean,
    modifier: Modifier = Modifier,
    onFrameSample: (frameTimeMs: Float, renderW: Int, renderH: Int) -> Unit
) {
    val context = LocalContext.current
    val latestCallback = rememberUpdatedState(onFrameSample)

    val renderer = remember {
        MaliStressRenderer { ms, w, h ->
            latestCallback.value(ms, w, h)
        }
    }

    val glView = remember {
        GLSurfaceView(context).apply {
            setEGLContextClientVersion(3)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
        }
    }

    DisposableEffect(isRunning, resolutionScale, useFp16Valhall) {
        renderer.useFp16 = useFp16Valhall
        renderer.targetScale = resolutionScale.coerceIn(0.45f, 1.0f)
        glView.post {
            val baseW = glView.width
            val baseH = glView.height
            if (baseW > 0 && baseH > 0) {
                val scaledW = (baseW * renderer.targetScale).roundToInt().coerceAtLeast(160)
                val scaledH = (baseH * renderer.targetScale).roundToInt().coerceAtLeast(120)
                glView.holder.setFixedSize(scaledW, scaledH)
                renderer.reportedWidth = scaledW
                renderer.reportedHeight = scaledH
            }
            glView.renderMode = if (isRunning) {
                GLSurfaceView.RENDERMODE_CONTINUOUSLY
            } else {
                GLSurfaceView.RENDERMODE_WHEN_DIRTY
            }
            if (!isRunning) {
                glView.requestRender()
            }
        }
        onDispose { }
    }

    DisposableEffect(Unit) {
        onDispose {
            glView.onPause()
        }
    }

    AndroidView(
        factory = { glView },
        modifier = modifier,
        update = { view ->
            if (isRunning) view.onResume()
        }
    )
}

private class MaliStressRenderer(
    private val onFrameMeasured: (Float, Int, Int) -> Unit
) : GLSurfaceView.Renderer {

    @Volatile var useFp16: Boolean = true
    @Volatile var targetScale: Float = 0.80f
    @Volatile var reportedWidth: Int = 0
    @Volatile var reportedHeight: Int = 0

    private var programFp16 = 0
    private var programHighp = 0
    private var uTimeLoc16 = -1
    private var uResLoc16 = -1
    private var uTimeLoc32 = -1
    private var uResLoc32 = -1

    private var startTimeNs = System.nanoTime()
    private var lastFrameNs = System.nanoTime()

    private val quadVertices: FloatBuffer = ByteBuffer.allocateDirect(8 * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(floatArrayOf(-1f, -1f, 1f, -1f, -1f, 1f, 1f, 1f))
            position(0)
        }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.027f, 0.043f, 0.07f, 1f)
        programFp16 = buildProgram(VERTEX_SHADER, buildFragmentShader(precision = "mediump"))
        programHighp = buildProgram(VERTEX_SHADER, buildFragmentShader(precision = "highp"))

        uTimeLoc16 = GLES30.glGetUniformLocation(programFp16, "uTime")
        uResLoc16 = GLES30.glGetUniformLocation(programFp16, "uRes")
        uTimeLoc32 = GLES30.glGetUniformLocation(programHighp, "uTime")
        uResLoc32 = GLES30.glGetUniformLocation(programHighp, "uRes")

        startTimeNs = System.nanoTime()
        lastFrameNs = startTimeNs
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        reportedWidth = width
        reportedHeight = height
    }

    override fun onDrawFrame(gl: GL10?) {
        val nowNs = System.nanoTime()
        val deltaMs = ((nowNs - lastFrameNs) / 1_000_000.0).toFloat()
        lastFrameNs = nowNs
        val elapsedSec = ((nowNs - startTimeNs) / 1_000_000_000.0).toFloat()

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        val prog = if (useFp16) programFp16 else programHighp
        val uTime = if (useFp16) uTimeLoc16 else uTimeLoc32
        val uRes = if (useFp16) uResLoc16 else uResLoc32

        if (prog != 0) {
            GLES30.glUseProgram(prog)
            GLES30.glUniform1f(uTime, elapsedSec)
            GLES30.glUniform2f(
                uRes,
                reportedWidth.coerceAtLeast(1).toFloat(),
                reportedHeight.coerceAtLeast(1).toFloat()
            )
            GLES30.glEnableVertexAttribArray(0)
            GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 0, quadVertices)
            GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
            GLES30.glDisableVertexAttribArray(0)
        }

        if (deltaMs > 0.2f && deltaMs < 1000f) {
            onFrameMeasured(deltaMs, reportedWidth, reportedHeight)
        }
    }

    private fun buildProgram(vsSrc: String, fsSrc: String): Int {
        val vs = compileShader(GLES30.GL_VERTEX_SHADER, vsSrc)
        val fs = compileShader(GLES30.GL_FRAGMENT_SHADER, fsSrc)
        if (vs == 0 || fs == 0) return 0
        val prog = GLES30.glCreateProgram()
        GLES30.glAttachShader(prog, vs)
        GLES30.glAttachShader(prog, fs)
        GLES30.glBindAttribLocation(prog, 0, "aPos")
        GLES30.glLinkProgram(prog)
        return prog
    }

    private fun compileShader(type: Int, src: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, src)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        return if (status[0] == 0) {
            GLES30.glDeleteShader(shader)
            0
        } else shader
    }

    companion object {
        private const val VERTEX_SHADER = """#version 300 es
            layout(location = 0) in vec2 aPos;
            void main() {
                gl_Position = vec4(aPos, 0.0, 1.0);
            }
        """

        private fun buildFragmentShader(precision: String): String = """#version 300 es
            precision $precision float;
            uniform float uTime;
            uniform vec2 uRes;
            out vec4 fragColor;

            float hash(vec2 p) {
                return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
            }

            float noise(vec2 p) {
                vec2 i = floor(p);
                vec2 f = fract(p);
                vec2 u = f * f * (3.0 - 2.0 * f);
                return mix(
                    mix(hash(i + vec2(0.0, 0.0)), hash(i + vec2(1.0, 0.0)), u.x),
                    mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), u.x),
                    u.y
                );
            }

            float fbm(vec2 p) {
                float v = 0.0;
                float a = 0.5;
                for (int i = 0; i < 6; i++) {
                    v += a * noise(p);
                    p = p * 2.04 + vec2(1.7, 9.2);
                    a *= 0.5;
                }
                return v;
            }

            void main() {
                vec2 uv = (gl_FragCoord.xy - 0.5 * uRes) / min(uRes.x, uRes.y);
                float t = uTime * 0.45;
                vec2 q = vec2(fbm(uv * 2.5 + t * 0.3), fbm(uv * 2.5 + vec2(5.2, 1.3)));
                float h = fbm(uv * 3.0 + q * 1.6 - t * 0.2);

                // Cel-shaded quantized bands reminiscent of Hyrule horizons
                float cel = floor(h * 5.0) / 5.0;
                vec3 deepSlate = vec3(0.03, 0.05, 0.09);
                vec3 cyanPulse = vec3(0.0, 0.90, 1.0);
                vec3 amberGlow = vec3(1.0, 0.70, 0.0);

                float ring = smoothstep(0.015, 0.0, abs(length(uv) - (0.35 + 0.06 * sin(uTime * 2.2))));
                float grid = smoothstep(0.95, 0.99, abs(sin(uv.x * 18.0 + q.x * 3.0))) +
                             smoothstep(0.95, 0.99, abs(sin(uv.y * 18.0 + q.y * 3.0)));

                vec3 col = mix(deepSlate, vec3(0.06, 0.16, 0.24), cel);
                col += cyanPulse * (ring * 0.85 + grid * 0.12);
                col += amberGlow * smoothstep(0.72, 0.92, h) * 0.55;

                fragColor = vec4(col, 1.0);
            }
        """
    }
}
