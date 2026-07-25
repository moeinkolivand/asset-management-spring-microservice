#!/bin/bash

# Usage: ./create-module.sh <new-module-name>
# Example: ./create-module.sh order

if [ -z "$1" ]; then
  echo "❌ Error: Please provide a module name."
  echo "Usage: ./create-module.sh <module-name>"
  exit 1
fi

NEW_MODULE="$1"
SOURCE_MODULE="user"   # change this if your base module is named differently

# Check if source module exists
if [ ! -d "$SOURCE_MODULE" ]; then
  echo "❌ Error: Source module '$SOURCE_MODULE' does not exist."
  exit 1
fi

# Check if target already exists
if [ -d "$NEW_MODULE" ]; then
  echo "❌ Error: Module '$NEW_MODULE' already exists."
  exit 1
fi

echo "📦 Creating new module: $NEW_MODULE (based on $SOURCE_MODULE)"

# 1. Copy the source module
cp -r "$SOURCE_MODULE" "$NEW_MODULE"

# 2. Update the new pom.xml
POM="$NEW_MODULE/pom.xml"
if [ -f "$POM" ]; then
  # Change artifactId, name, description
  sed -i "s|<artifactId>$SOURCE_MODULE</artifactId>|<artifactId>$NEW_MODULE</artifactId>|g" "$POM"
  sed -i "s|<name>$SOURCE_MODULE</name>|<name>$NEW_MODULE</name>|g" "$POM"
  sed -i "s|<description>$SOURCE_MODULE</description>|<description>$NEW_MODULE</description>|g" "$POM"

  # Optional: update the main class package if you want, but we keep it as is for now.
  echo "✅ Updated $POM"
else
  echo "⚠️ Warning: $POM not found, skipping POM update."
fi

# 3. Add the new module to the root pom.xml (if not already present)
ROOT_POM="pom.xml"
if [ -f "$ROOT_POM" ]; then
  # Check if module is already listed
  if grep -q "<module>$NEW_MODULE</module>" "$ROOT_POM"; then
    echo "ℹ️ Module '$NEW_MODULE' already present in root pom.xml"
  else
    # Insert the new module before the closing </modules> tag
    sed -i "/<\/modules>/i \ \ \ \ <module>$NEW_MODULE</module>" "$ROOT_POM"
    echo "✅ Added module '$NEW_MODULE' to root pom.xml"
  fi
else
  echo "⚠️ Warning: Root pom.xml not found at $ROOT_POM"
fi

echo "🎉 Module '$NEW_MODULE' created successfully!"
echo "Next steps:"
echo "  - Open IntelliJ and reload Maven projects."
echo "  - Update the application.yml in $NEW_MODULE/src/main/resources/ if needed."
echo "  - Rename the main class from UserApplication to ${NEW_MODULE^}Application (optional)."