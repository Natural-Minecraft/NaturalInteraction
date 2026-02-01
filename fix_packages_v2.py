import os

src_root = r"d:\NaturalSMP\plugin\NaturalInteraction\src\main\java\id\naturalsmp\naturalinteraction"

def fix_file(file_path):
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    new_content = content.replace("com.natural.interaction", "id.naturalsmp.naturalinteraction")
    
    # Specific case for singular/plural subpacks if needed
    # (But based on the tree, they are in folders like 'gui', 'manager')
    # If the file is in 'utils' but its package line says '.util', fix it.
    
    # Logic to fix package based on folder
    rel_path = os.path.relpath(file_path, r"d:\NaturalSMP\plugin\NaturalInteraction\src\main\java")
    expected_package = rel_path.replace(os.sep, ".").replace(".java", "")
    # Remove the class name from package
    expected_package = ".".join(expected_package.split(".")[:-1])
    
    lines = new_content.splitlines()
    if lines and lines[0].startswith("package "):
        lines[0] = f"package {expected_package};"
    
    new_content = "\n".join(lines)
    
    if content != new_content:
        with open(file_path, "w", encoding="utf-8") as f:
            f.write(new_content)
        print(f"Fixed: {file_path}")

for root, dirs, files in os.walk(src_root):
    for file in files:
        if file.endswith(".java"):
            fix_file(os.path.join(root, file))
