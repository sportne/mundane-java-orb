#include <tao/corba.h>
#include <tao/DynamicInterface/Dynamic_Implementation.h>
#include <tao/DynamicInterface/Server_Request.h>
#include <tao/PortableServer/PortableServer.h>

#include <cstdlib>
#include <fstream>
#include <iostream>
#include <stdexcept>
#include <string>

namespace {

class SmokeServant final : public TAO_DynamicImplementation {
 public:
  explicit SmokeServant(CORBA::ORB_ptr orb) : orb_(CORBA::ORB::_duplicate(orb)) {}

  void invoke(CORBA::ServerRequest_ptr request) override {
    CORBA::NVList_ptr arguments;
    orb_->create_list(0, arguments);
    request->arguments(arguments);

    if (std::string(request->operation()) == "_non_existent") {
      CORBA::Any result;
      result <<= CORBA::Any::from_boolean(false);
      request->set_result(result);
      return;
    }
    throw CORBA::BAD_OPERATION();
  }

  CORBA::RepositoryId _primary_interface(
      const PortableServer::ObjectId&,
      PortableServer::POA_ptr) override {
    return CORBA::string_dup("IDL:interop/basic/Smoke:1.0");
  }

 private:
  CORBA::ORB_var orb_;
};

std::string env_or_default(const char* name, const std::string& fallback) {
  const char* value = std::getenv(name);
  if (value == nullptr || *value == '\0') {
    return fallback;
  }
  return value;
}

std::string ior_path(const std::string& scenario) {
  return env_or_default("INTEROP_IORS_DIR", "/interop/iors") + "/" + scenario + "-server.ior";
}

void write_file(const std::string& path, const std::string& value) {
  std::ofstream out(path);
  if (!out) {
    throw std::runtime_error("failed to open output file: " + path);
  }
  out << value << '\n';
}

std::string read_file(const std::string& path) {
  std::ifstream in(path);
  if (!in) {
    throw std::runtime_error("failed to open input file: " + path);
  }
  std::string value;
  std::getline(in, value);
  if (value.empty()) {
    throw std::runtime_error("input file is empty: " + path);
  }
  return value;
}

void write_report(const std::string& role, const std::string& scenario, const std::string& status,
                  const std::string& classification, const std::string& message) {
  const std::string path =
      env_or_default("INTEROP_REPORTS_DIR", "/interop/reports") + "/" + scenario + "-" + role +
      ".ace-tao.json";
  std::ofstream out(path);
  if (!out) {
    throw std::runtime_error("failed to open report file: " + path);
  }
  out << "{\n"
      << "  \"peer\": \"ace-tao\",\n"
      << "  \"role\": \"" << role << "\",\n"
      << "  \"scenario\": \"" << scenario << "\",\n"
      << "  \"status\": \"" << status << "\",\n"
      << "  \"classification\": \"" << classification << "\",\n"
      << "  \"message\": \"" << message << "\"\n"
      << "}\n";
}

int run_server(int argc, char* argv[], const std::string& scenario) {
  CORBA::ORB_var orb = CORBA::ORB_init(argc, argv);
  CORBA::Object_var root = orb->resolve_initial_references("RootPOA");
  PortableServer::POA_var poa = PortableServer::POA::_narrow(root.in());
  if (CORBA::is_nil(poa.in())) {
    throw std::runtime_error("RootPOA narrow returned nil");
  }
  SmokeServant servant(orb.in());
  PortableServer::ObjectId_var object_id = poa->activate_object(&servant);
  CORBA::Object_var object = poa->id_to_reference(object_id.in());
  PortableServer::POAManager_var manager = poa->the_POAManager();
  manager->activate();
  CORBA::String_var ior = orb->object_to_string(object.in());
  write_file(ior_path(scenario), ior.in());
  write_report("server", scenario, "passed", "server-ready",
               "ACE/TAO ORB wrote a RootPOA object reference and entered its event loop");
  orb->run();
  orb->destroy();
  return 0;
}

int check_object(int argc, char* argv[], const std::string& role, const std::string& scenario) {
  CORBA::ORB_var orb = CORBA::ORB_init(argc, argv);
  const std::string ior = read_file(ior_path(scenario));
  CORBA::Object_var object = orb->string_to_object(ior.c_str());
  if (CORBA::is_nil(object.in())) {
    throw std::runtime_error("string_to_object returned nil");
  }
  if (object->_non_existent()) {
    throw std::runtime_error("remote object reported non-existence");
  }
  write_report(role, scenario, "passed", "object-reference-checked",
               "ACE/TAO invoked a live _non_existent request through the scenario IOR");
  orb->destroy();
  return 0;
}

}  // namespace

int main(int argc, char* argv[]) {
  const std::string role = argc > 1 ? argv[1] : env_or_default("INTEROP_ROLE", "manual");
  const std::string scenario = env_or_default("INTEROP_SCENARIO", "manual");
  try {
    if (role == "server") {
      return run_server(argc, argv, scenario);
    }
    if (role == "client" || role == "health") {
      return check_object(argc, argv, role, scenario);
    }
    std::cerr << "unsupported ACE/TAO peer role: " << role << '\n';
    return 64;
  } catch (const CORBA::Exception& ex) {
    std::cerr << "ACE/TAO CORBA exception: " << ex._name() << '\n';
    return 1;
  } catch (const std::exception& ex) {
    std::cerr << "ACE/TAO peer failure: " << ex.what() << '\n';
    return 1;
  }
}
