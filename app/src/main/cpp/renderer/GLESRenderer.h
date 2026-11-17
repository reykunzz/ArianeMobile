#pragma once
#include <vector>
#include <mutex>

namespace ariane {
class GLESRenderer {
public:
    void initialize();
    void draw(int width, int height);
    void shutdown();
    void setPoints(const std::vector<float>& points);
    void setCamera(float rotationX, float rotationY, float zoom);

private:
    unsigned int program{};
    unsigned int vao{};
    unsigned int vbo{};
    int positionLocation{-1};
    int colorLocation{-1};
    int rotationLocation{-1};
    int zoomLocation{-1};
    std::vector<float> points;
    std::mutex dataMutex;
    float rotationX{0.65f};
    float rotationY{0.55f};
    float zoom{1.0f};
};
}
