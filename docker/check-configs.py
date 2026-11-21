import sys

try:
    import yaml
except ImportError:
    print("PyYAML 未安装，跳过 YAML 校验")
    sys.exit(0)

for path in ["docker-compose.yml", ".github/workflows/docker.yml", ".github/workflows/build.yml"]:
    with open(path, encoding="utf-8") as f:
        data = yaml.safe_load(f)
    print(f"{path}: OK ({type(data).__name__})")

compose = yaml.safe_load(open("docker-compose.yml", encoding="utf-8"))
svc = compose["services"]["thepit"]
assert svc["image"].startswith("ghcr.io/"), "image 必须是 ghcr.io"
assert svc["ports"] == ["25565:25565"]
assert svc["volumes"] == ["thepit-data:/data"]
env = svc["environment"]
assert "ONLINE_MODE" in env and "WHITE_LIST" in env and "ALLOW_NETHER" in env
print("compose 关键字段校验通过")

wf = yaml.safe_load(open(".github/workflows/docker.yml", encoding="utf-8"))
steps = wf["jobs"]["build"]["steps"]
names = [s.get("name") or s.get("uses") for s in steps]
uses = [s.get("uses", "") for s in steps]
print("docker workflow steps:", names)
assert any("build-push-action" in u for u in uses), "缺少 build-push-action"
assert wf["permissions"]["packages"] == "write"
print("docker workflow 关键步骤校验通过")
