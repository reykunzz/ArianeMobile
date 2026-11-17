#include "IPLReader.h"
#include <algorithm>
#include <cctype>
#include <sstream>
#include <utility>

namespace ariane::formats {
namespace {
std::string trim(std::string value) {
    const auto first = value.find_first_not_of(" \t\r\n");
    if (first == std::string::npos) return {};
    const auto last = value.find_last_not_of(" \t\r\n");
    return value.substr(first, last - first + 1);
}

bool parseRow(const std::string& line, IplInstance& out) {
    std::stringstream stream(line);
    std::string field;
    std::vector<std::string> fields;
    while (std::getline(stream, field, ',')) fields.push_back(trim(field));
    if (fields.size() < 11) return false;
    try {
        out.modelId = std::stoi(fields[0]);
        out.modelName = fields[1];
        out.interior = std::stoi(fields[2]);
        out.x = std::stof(fields[3]); out.y = std::stof(fields[4]); out.z = std::stof(fields[5]);
        out.rx = std::stof(fields[6]); out.ry = std::stof(fields[7]);
        out.rz = std::stof(fields[8]); out.rw = std::stof(fields[9]);
        out.lodIndex = std::stoi(fields[10]);
    } catch (...) { return false; }
    return !out.modelName.empty();
}
} // namespace

IplDocument parseTextIpl(const std::string& source) {
    IplDocument document;
    std::istringstream input(source);
    std::string line;
    bool inInstances = false;
    size_t lineNumber = 0;
    while (std::getline(input, line)) {
        ++lineNumber;
        line = trim(line);
        if (line.empty() || line[0] == '#') continue;
        std::string lower = line;
        std::transform(lower.begin(), lower.end(), lower.begin(),
            [](unsigned char c) { return static_cast<char>(std::tolower(c)); });
        if (lower == "inst") { inInstances = true; continue; }
        if (lower == "end") { inInstances = false; continue; }
        if (!inInstances) continue;
        IplInstance instance;
        if (parseRow(line, instance)) document.instances.push_back(std::move(instance));
        else document.warnings.push_back("Invalid inst row at line " + std::to_string(lineNumber));
    }
    return document;
}
} // namespace ariane::formats
