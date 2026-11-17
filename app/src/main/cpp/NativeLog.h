#pragma once

#include <android/log.h>
#include <mutex>
#include <string>
#include <vector>
#include <sstream>

namespace ariane {

class NativeLog {
public:
    static NativeLog& instance() {
        static NativeLog value;
        return value;
    }

    void add(const std::string& message) {
        std::lock_guard<std::mutex> lock(mutex);
        entries.push_back(message);

        if (entries.size() > 500) {
            entries.erase(entries.begin(), entries.begin() + 100);
        }

        __android_log_print(
            ANDROID_LOG_INFO,
            "SAMapEditor",
            "%s",
            message.c_str()
        );
    }

    std::string get() {
        std::lock_guard<std::mutex> lock(mutex);
        std::ostringstream out;

        for (const auto& entry : entries) {
            out << entry << '\n';
        }

        return out.str();
    }

private:
    std::mutex mutex;
    std::vector<std::string> entries;
};

inline void addNativeLog(const std::string& message) {
    NativeLog::instance().add(message);
}

inline std::string getNativeLogs() {
    return NativeLog::instance().get();
}

}
