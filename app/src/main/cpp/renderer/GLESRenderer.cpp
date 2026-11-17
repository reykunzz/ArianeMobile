#include "GLESRenderer.h"
#include <GLES3/gl3.h>
namespace ariane {
void GLESRenderer::initialize() { glClearColor(0.075f, 0.09f, 0.12f, 1.0f); }
void GLESRenderer::draw(int width, int height) {
    glViewport(0, 0, width, height);
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
}
void GLESRenderer::shutdown() {}
}
