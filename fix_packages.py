import os

root_dir = r"d:\NaturalSMP\plugin\NaturalInteraction\src\main\java\id\naturalsmp\naturalinteraction"
legacy_subpacks = ["command", "gui", "hook", "listener", "manager", "model", "util"]

for subpack in legacy_subpacks:
    subpack_path = os.path.join(root_dir, subpack)
    if not os.path.exists(subpack_path):
        continue
    
    for filename in os.listdir(subpack_path):
        if filename.endswith(".java"):
            file_path = os.path.join(subpack_path, filename)
            with open(file_path, "r", encoding="utf-8") as f:
                content = f.read()
            
            # Update package
            content = content.replace("package com.natural.interaction", "package id.naturalsmp.naturalinteraction")
            
            # Update imports
            content = content.replace("import com.natural.interaction", "import id.naturalsmp.naturalinteraction")
            
            with open(file_path, "w", encoding="utf-8") as f:
                f.write(content)

# Also fix the legacy main class if it exists in the root
main_class_path = os.path.join(root_dir, "NaturalInteraction_LEGACY.java") # I'll assume it was renamed or needs check
# Wait, let's just check the root NaturalInteraction.java
main_class_path = os.path.join(root_dir, "NaturalInteraction.java")
with open(main_class_path, "r", encoding="utf-8") as f:
    content = f.read()

content = content.replace("package com.natural.interaction;", "package id.naturalsmp.naturalinteraction;")
content = content.replace("import com.natural.interaction", "import id.naturalsmp.naturalinteraction")

with open(main_class_path, "w", encoding="utf-8") as f:
    f.write(content)
