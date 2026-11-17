#include <string_view>
namespace ariane::engine { std::string_view integrationStatus() noexcept {
#ifdef ARIANE_UPSTREAM_CORE_ENABLED
return "Android-adapted Ariane core enabled";
#else
return "Android adapter active; upstream core pending";
#endif
} }
