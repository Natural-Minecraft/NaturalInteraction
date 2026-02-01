import os

src_base = r"d:\NaturalSMP\plugin\NaturalInteraction\src\main\java"
project_root = os.path.join(src_base, "id", "naturalsmp", "naturalinteraction")

def fix_file(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    # 1. Broad replacements for common mistakes
    new_content = content.replace("id.naturalsmp.naturalinteraction.interaction.", "id.naturalsmp.naturalinteraction.")
    new_content = new_content.replace("id.naturalsmp.naturalinteraction.util.", "id.naturalsmp.naturalinteraction.utils.")
    
    # Ensure id.naturalsmp.naturalinteraction.util (no dot) is also caught if it's an import
    new_content = new_content.replace("import id.naturalsmp.naturalinteraction.util;", "import id.naturalsmp.naturalinteraction.utils.*;")

    # 2. Fix package declaration based on ACTUAL directory
    rel_path = os.path.relpath(file_path, src_base)
    expected_package = rel_path.replace(os.sep, ".").replace(".java", "")
    expected_package = ".".join(expected_package.split(".")[:-1])
    
    lines = new_content.splitlines()
    if lines:
        for i in range(len(lines)):
            if lines[i].strip().startswith("package "):
                lines[i] = f"package {expected_package};"
                break
    
    final_content = "\n".join(lines) + "\n"
    
    if content != final_content:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(final_content)
        print(f"Fixed: {file_path}")

# Fix all files in project_root
for root, dirs, files in os.walk(project_root):
    for file in files:
        if file.endswith(".java"):
            fix_file(os.path.join(root, file))

# Fix filename casing for NaturalInteraction.java
# We need to be careful with Windows case-insensitivity.
target_main = os.path.join(project_root, "NaturalInteraction.java")
# Look for any file that is Naturalinteraction.java (case insensitive)
for f in os.listdir(project_root):
    if f.lower() == "naturalinteraction.java" and f != "NaturalInteraction.java":
        old_path = os.path.join(project_root, f)
        temp_path = os.path.join(project_root, "NaturalInteractionTemp.java")
        os.rename(old_path, temp_path)
        os.rename(temp_path, target_main)
        print(f"Renamed {f} to NaturalInteraction.java")
