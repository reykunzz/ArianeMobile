#pragma once
#include <cstdint>
#include <string>
#include <vector>
namespace ariane::formats { struct ImgEntry { std::string name; uint32_t offsetBytes{},sizeBytes{}; }; struct ImgIndex { bool valid{false}; std::string error; std::vector<ImgEntry> entries; }; ImgIndex parseImgV2Directory(const std::vector<uint8_t>& bytes); }
