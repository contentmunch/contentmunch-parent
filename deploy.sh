#!/bin/bash

set -euo pipefail
# CONFIG: ADD bom config
BOM_MODULE="contentmunch-bom"
BOM_COORDINATES="com.contentmunch:contentmunch-bom"

# Step 1: Verify the build
echo "🛠️ Verifying build with quality profile..."
mvn -B verify -Pquality

# Step 2: Bump minor version
echo "🔢 Bumping minor version..."
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT_VERSION"
NEW_VERSION="${MAJOR}.$((MINOR + 1)).0"

# Step 2a: Bump BOM module version
echo "📘 Bumping BOM version to $NEW_VERSION in $BOM_MODULE..."
pushd "$BOM_MODULE" > /dev/null
mvn --batch-mode versions:set -DnewVersion=$NEW_VERSION
mvn versions:commit
popd > /dev/null

# Step 2b: Update parent POM to use new BOM version
echo "🧩 Updating parent POM to use BOM version $NEW_VERSION..."
mvn --batch-mode versions:use-dep-version -Dincludes=$BOM_COORDINATES -DdepVersion=$NEW_VERSION -DforceVersion=true -DgenerateBackupPoms=false

# Step 2c: Set project version
echo "📝 Setting parent version to $NEW_VERSION..."
mvn --batch-mode versions:set -DnewVersion=$NEW_VERSION
mvn versions:commit

echo "✅ New version set: $NEW_VERSION"

# Step 3: Prompt for commit message
echo ""
read -rp "✍️  Enter your commit message: " COMMIT_MSG

# Step 4: Commit and push version bump
echo "📦 Committing with message: $COMMIT_MSG"
git commit -am "$COMMIT_MSG"
git push origin main

# Step 5: Deploy to Nexus
echo "🚀 Deploying to Nexus..."

# Deploy BOM first
mvn deploy -pl contentmunch-bom -DskipTests

# Deploy everything else
mvn deploy -pl !contentmunch-bom -DskipTests

echo "🎉 Release complete: $NEW_VERSION"

