#pragma once
namespace ariane {
class GLESRenderer {
public:
    void initialize();
    void draw(int width, int height);
    void shutdown();
};
}
