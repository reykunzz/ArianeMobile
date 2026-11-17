#include "NativeLog.h"
#include "GLESRenderer.h"
#include <GLES3/gl3.h>
#include <android/log.h>
#include <algorithm>
#include <cmath>
#include <vector>

namespace ariane {
namespace {
GLuint compileShader(GLenum type, const char* source) {
    GLuint shader = glCreateShader(type);
    glShaderSource(shader, 1, &source, nullptr);
    glCompileShader(shader);

    GLint ok = GL_FALSE;
    glGetShaderiv(shader, GL_COMPILE_STATUS, &ok);
    if (!ok) {
        GLint length = 0;
        glGetShaderiv(shader, GL_INFO_LOG_LENGTH, &length);
        std::vector<char> log(static_cast<size_t>(length) + 1);
        glGetShaderInfoLog(shader, length, nullptr, log.data());
        std::string error = std::string("OpenGL shader compile failed: ") + log.data();
        addNativeLog(error);
        __android_log_print(ANDROID_LOG_ERROR, "SAMapEditor",
                            "Shader compile failed: %s", log.data());
        glDeleteShader(shader);
        return 0;
    }
    return shader;
}

GLuint makeProgram() {
    const char* vertex = R"(#version 300 es
        layout(location=0) in vec3 aPosition;
        uniform vec2 uRotation;
        uniform float uZoom;
        void main() {
            float cy = cos(uRotation.y);
            float sy = sin(uRotation.y);
            float cx = cos(uRotation.x);
            float sx = sin(uRotation.x);

            // GTA SA: X-Y adalah bidang tanah, Z adalah ketinggian.
            float x = aPosition.x;
            float y = aPosition.y;
            float z = aPosition.z;

            // Rotasi mengelilingi sumbu Z (vertikal GTA).
            float groundX = x * cy - y * sy;
            float groundY = x * sy + y * cy;

            // Sudut pandang kamera: miringkan bidang tanah.
            float screenY = groundY * cx - z * sx;
            float depth = groundY * sx + z * cx;

            gl_Position = vec4(groundX * uZoom,
                               screenY * uZoom,
                               depth * 0.25, 1.0);
            gl_PointSize = 6.0;
        }
    )";

    const char* fragment = R"(#version 300 es
        precision mediump float;
        uniform vec4 uColor;
        out vec4 outColor;
        void main() {
            outColor = uColor;
        }
    )";

    GLuint vs = compileShader(GL_VERTEX_SHADER, vertex);
    GLuint fs = compileShader(GL_FRAGMENT_SHADER, fragment);
    if (!vs || !fs) return 0;

    GLuint p = glCreateProgram();
    glAttachShader(p, vs);
    glAttachShader(p, fs);
    glLinkProgram(p);
    glDeleteShader(vs);
    glDeleteShader(fs);

    GLint ok = GL_FALSE;
    glGetProgramiv(p, GL_LINK_STATUS, &ok);
    if (!ok) {
        GLint length = 0;
        glGetProgramiv(p, GL_INFO_LOG_LENGTH, &length);
        std::vector<char> log(static_cast<size_t>(length) + 1);
        glGetProgramInfoLog(p, length, nullptr, log.data());
        std::string error = std::string("OpenGL program link failed: ") + log.data();
        addNativeLog(error);
        __android_log_print(ANDROID_LOG_ERROR, "SAMapEditor",
                            "Program link failed: %s", log.data());
        glDeleteProgram(p);
        return 0;
    }
    return p;
}
}

void GLESRenderer::initialize() {
    addNativeLog("Renderer: initialize dimulai");

    if (program) glDeleteProgram(program);
    if (vbo) glDeleteBuffers(1, &vbo);
    if (vao) glDeleteVertexArrays(1, &vao);

    program = makeProgram();

    if (!program) {
        addNativeLog("Renderer: GAGAL membuat program OpenGL");
        return;
    }

    positionLocation = 0;
    colorLocation = glGetUniformLocation(program, "uColor");
    rotationLocation = glGetUniformLocation(program, "uRotation");
    zoomLocation = glGetUniformLocation(program, "uZoom");

    glGenVertexArrays(1, &vao);
    glGenBuffers(1, &vbo);

    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);

    glEnableVertexAttribArray(positionLocation);
    glVertexAttribPointer(
        positionLocation, 3, GL_FLOAT, GL_FALSE, 0, nullptr
    );

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);

    glClearColor(0.055f, 0.075f, 0.11f, 1.0f);
    glEnable(GL_DEPTH_TEST);

    addNativeLog("Renderer: VAO dan VBO berhasil dibuat");
    addNativeLog("Renderer: initialize selesai");
}

void GLESRenderer::setPoints(const std::vector<float>& input) {
    addNativeLog("Renderer: menerima " + std::to_string(input.size() / 3) + " titik IPL");
    std::vector<float> normalized;
    if (input.size() >= 3) {
        float minX=input[0], maxX=input[0];
        float minY=input[1], maxY=input[1];
        float minZ=input[2], maxZ=input[2];

        for (size_t i=0; i+2<input.size(); i+=3) {
            minX=std::min(minX,input[i]);
            maxX=std::max(maxX,input[i]);
            minY=std::min(minY,input[i+1]);
            maxY=std::max(maxY,input[i+1]);
            minZ=std::min(minZ,input[i+2]);
            maxZ=std::max(maxZ,input[i+2]);
        }

        float cx=(minX+maxX)*0.5f;
        float cy=(minY+maxY)*0.5f;
        float cz=(minZ+maxZ)*0.5f;
        float extent=std::max({maxX-minX,maxY-minY,maxZ-minZ,1.0f});
        float scale=1.7f/extent;

        normalized.reserve(input.size());
        for (size_t i=0; i+2<input.size(); i+=3) {
            normalized.push_back((input[i]-cx)*scale);
            normalized.push_back((input[i+1]-cy)*scale);
            normalized.push_back((input[i+2]-cz)*scale);
        }
    }

    std::lock_guard<std::mutex> lock(dataMutex);
    points = std::move(normalized);
}

void GLESRenderer::setCamera(float x, float y, float z) {
    std::lock_guard<std::mutex> lock(dataMutex);
    rotationX=x;
    rotationY=y;
    zoom=std::clamp(z,0.25f,5.0f);
}

void GLESRenderer::draw(int width, int height) {
    static bool firstDrawLogged = false;
    if (!firstDrawLogged) {
        addNativeLog("Renderer: draw pertama, viewport " + std::to_string(width) + "x" + std::to_string(height));
        firstDrawLogged = true;
    }
    if (width<=0 || height<=0) return;

    auto checkGL = [](const char* stage) {
        GLenum error = glGetError();
        if (error != GL_NO_ERROR) {
            addNativeLog(
                std::string("GL ERROR pada ") + stage +
                ": " + std::to_string(static_cast<unsigned int>(error))
            );
        }
    };

    checkGL("sebelum draw");

    glViewport(0,0,width,height);
    checkGL("glViewport");

    glClearColor(0.055f, 0.075f, 0.11f, 1.0f);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    checkGL("glClear");

    if (!program) return;

    std::vector<float> pointCopy;
    float rx,ry,z;

    {
        std::lock_guard<std::mutex> lock(dataMutex);
        pointCopy=points;
        rx=rotationX;
        ry=rotationY;
        z=zoom;
    }

    glUseProgram(program);
    glUniform2f(rotationLocation,rx,ry);
    glUniform1f(zoomLocation,z);
    checkGL("uniform kamera");

    // Grid on the GTA ground plane (X-Y), at Z = 0.
    std::vector<float> grid;
    for (int i=-10;i<=10;i++) {
        float v=i/10.0f;
        grid.insert(grid.end(),{-1.0f,v,0.0f, 1.0f,v,0.0f});
        grid.insert(grid.end(),{v,-1.0f,0.0f, v,1.0f,0.0f});
    }

    if (!vao || !vbo) return;

    glBindVertexArray(vao);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);

    glBufferData(GL_ARRAY_BUFFER,
        static_cast<GLsizeiptr>(grid.size()*sizeof(float)),
        grid.data(),GL_DYNAMIC_DRAW);
    checkGL("buffer grid");
    glUniform4f(colorLocation,0.22f,0.29f,0.38f,1.0f);
    glDrawArrays(GL_LINES,0,static_cast<GLsizei>(grid.size()/3));
    checkGL("draw grid");

    // Coordinate axes
    const float axes[] = {
        -1,0,0, 1,0,0,
        0,-1,0, 0,1,0,
        0,0,-1, 0,0,1
    };
    glBufferData(GL_ARRAY_BUFFER,sizeof(axes),axes,GL_STATIC_DRAW);
    checkGL("buffer axes");
    glUniform4f(colorLocation,0.85f,0.35f,0.30f,1.0f);
    glDrawArrays(GL_LINES,0,2);
    glUniform4f(colorLocation,0.35f,0.85f,0.45f,1.0f);
    glDrawArrays(GL_LINES,2,2);
    glUniform4f(colorLocation,0.35f,0.60f,1.0f,1.0f);
    glDrawArrays(GL_LINES,4,2);
    checkGL("draw axes");

    // IPL instance positions
    if (!pointCopy.empty()) {
        glBufferData(GL_ARRAY_BUFFER,
            static_cast<GLsizeiptr>(pointCopy.size()*sizeof(float)),
            pointCopy.data(),GL_DYNAMIC_DRAW);
        checkGL("buffer IPL");
        glUniform4f(colorLocation,1.0f,0.78f,0.25f,1.0f);
        glDrawArrays(GL_POINTS,0,
            static_cast<GLsizei>(pointCopy.size()/3));
        checkGL("draw IPL");
    }

    glBindBuffer(GL_ARRAY_BUFFER, 0);
    glBindVertexArray(0);

    static size_t lastLoggedCount = SIZE_MAX;
    size_t currentCount = pointCopy.size() / 3;

    if (currentCount != lastLoggedCount) {
        GLenum error = glGetError();

        addNativeLog(
            "Renderer: jumlah titik saat draw=" +
            std::to_string(currentCount) +
            ", GL error=" +
            std::to_string(static_cast<unsigned int>(error))
        );

        lastLoggedCount = currentCount;
    }
}

void GLESRenderer::shutdown() {
    if (vbo) {
        glDeleteBuffers(1, &vbo);
        vbo = 0;
    }

    if (vao) {
        glDeleteVertexArrays(1, &vao);
        vao = 0;
    }

    if (program) {
        glDeleteProgram(program);
        program = 0;
    }

    addNativeLog("Renderer: seluruh resource GPU dibersihkan");
}
}
