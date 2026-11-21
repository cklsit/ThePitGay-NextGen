import io
import zipfile

jar = r"core/build/libs/ThePitGay-V.jar"
with zipfile.ZipFile(jar) as z:
    inner = z.read("maps/pit_arena.zip")
print("inner zip bytes:", len(inner))
with zipfile.ZipFile(io.BytesIO(inner)) as z2:
    names = z2.namelist()
    tops = sorted({n.split('/')[0] for n in names})
    print("top-level:", tops)
    regions = [n for n in names if n.startswith('world/region/')]
    print("region files:", len(regions))
    print("level.dat present:", any(n == 'world/level.dat' for n in names))
    print("uid.dat bundled:", any(n.endswith('uid.dat') for n in names))
    print("session.lock bundled:", any(n.endswith('session.lock') for n in names))
