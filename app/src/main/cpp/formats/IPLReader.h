#pragma once
#include <string>
#include <vector>
namespace ariane::formats {
struct IplInstance { int modelId{}; std::string modelName; int interior{}; float x{},y{},z{},rx{},ry{},rz{},rw{}; int lodIndex{-1}; };
struct IplDocument { std::vector<IplInstance> instances; std::vector<std::string> warnings; };
IplDocument parseTextIpl(const std::string& source);
}
