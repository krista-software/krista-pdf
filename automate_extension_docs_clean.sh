#!/bin/bash

# Extension Documentation Automation Script
# Uses Augment CLI (auggie) to rewrite extension documentation according to guidelines
# Author: Krista Documentation Team
# Date: October 4, 2025
# Jai Bhavani, Jai Shivaji

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
WORK_DIR="documentation_review"
REPO_URL=$(git config --get remote.origin.url)
GUIDELINES_FILE="/Users/deepakshingan/Desktop/ExtensionDocumentationGuidelines.md"

# Banner
echo -e "${BLUE}"
echo "╔════════════════════════════════════════════════════════════╗"
echo "║   Extension Documentation Automation Script               ║"
echo "║   Jai Bhavani, Jai Shivaji                                ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# Check prerequisites
check_prerequisites() {
    echo -e "${YELLOW}Checking prerequisites...${NC}"
    
    # Check if auggie is installed
    if ! command -v auggie &> /dev/null; then
        echo -e "${RED}Error: Augment CLI (auggie) is not installed${NC}"
        echo "Please install auggie first: https://docs.augmentcode.com/cli"
        exit 1
    fi
    echo -e "${GREEN}✓ auggie is installed${NC}"
    
    # Check if git is installed
    if ! command -v git &> /dev/null; then
        echo -e "${RED}Error: git is not installed${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ git is installed${NC}"
    
    # Check if we're in a git repository
    if ! git rev-parse --git-dir > /dev/null 2>&1; then
        echo -e "${RED}Error: Not in a git repository${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ In git repository${NC}"
    
    echo -e "${GREEN}✓ All prerequisites met${NC}\n"
}

# Get list of release branches
get_release_branches() {
    echo -e "${YELLOW}Fetching release branches...${NC}"
    git fetch --all --prune
    
    # Get all release/* branches
    BRANCHES=$(git branch -r | grep 'origin/release/' | sed 's/origin\///' | sed 's/^[[:space:]]*//')
    
    if [ -z "$BRANCHES" ]; then
        echo -e "${RED}No release branches found${NC}"
        exit 1
    fi
    
    echo -e "${GREEN}Found release branches:${NC}"
    echo "$BRANCHES" | nl
    echo ""
}

# Select branch to process
select_branch() {
    echo -e "${YELLOW}Select a branch to process (or 'q' to quit, 'a' for all):${NC}"
    read -p "Enter selection: " selection
    
    if [ "$selection" = "q" ]; then
        echo -e "${BLUE}Exiting...${NC}"
        exit 0
    fi
    
    if [ "$selection" = "a" ]; then
        SELECTED_BRANCHES="$BRANCHES"
        PROCESS_ALL=true
    else
        SELECTED_BRANCHES=$(echo "$BRANCHES" | sed -n "${selection}p")
        PROCESS_ALL=false
    fi
    
    if [ -z "$SELECTED_BRANCHES" ]; then
        echo -e "${RED}Invalid selection${NC}"
        exit 1
    fi
}

# Process a single extension
process_extension() {
    local branch=$1
    local extension_name=$(echo "$branch" | sed 's/release\///')
    
    echo -e "${BLUE}"
    echo "╔════════════════════════════════════════════════════════════╗"
    echo "║   Processing: $extension_name"
    echo "╚════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    # Create work directory
    mkdir -p "$WORK_DIR"
    cd "$WORK_DIR"
    
    # Clone repository if not exists
    if [ ! -d "krista-global-catalog" ]; then
        echo -e "${YELLOW}Cloning repository...${NC}"
        git clone "$REPO_URL" krista-global-catalog
    fi
    
    cd krista-global-catalog
    
    # Checkout release branch
    echo -e "${YELLOW}Checking out branch: $branch in $(pwd)${NC}"
    git fetch origin
    git checkout "$branch"
    git pull origin "$branch"
    
    # Create docs branch
    local docs_branch="docs/$extension_name"
    echo -e "${YELLOW}Creating documentation branch: $docs_branch${NC}"
    
    # Delete local branch if it already exists
    if git show-ref --verify --quiet refs/heads/"$docs_branch"; then
        echo -e "${YELLOW}Branch $docs_branch already exists locally. Deleting...${NC}"
        git branch -D "$docs_branch"
        echo -e "${GREEN}✓ Deleted existing local branch${NC}"
    fi
    
    git checkout -b "$docs_branch"
    echo -e "${GREEN}✓ Created new branch: $docs_branch${NC}"
    
    # Check if guidelines file exists
    if [ ! -f "$GUIDELINES_FILE" ]; then
        echo -e "${RED}Warning: Guidelines file not found at $GUIDELINES_FILE${NC}"
        echo -e "${YELLOW}Continuing anyway...${NC}"
    fi
    
    # Execute auggie command interactively
    echo -e "${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   Starting Interactive Documentation Rewrite              ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
    echo -e "${YELLOW}Extension: $extension_name${NC}"
    echo -e "${YELLOW}This may take 10-20 minutes...${NC}"
    echo -e "${BLUE}💬 You can provide feedback to auggie during execution${NC}"
    echo -e "${BLUE}📝 Auggie will ask for your input when needed${NC}\n"
    
    # Run auggie interactively
    auggie chat "rewrite documentation as per Rules and guidelines"
    
    # Check if changes were made
    if git diff --quiet && git diff --cached --quiet; then
        echo -e "${YELLOW}No changes detected. Skipping this extension.${NC}"
        cd ../..
        return
    fi
    
    # Show summary of changes
    echo -e "\n${GREEN}Documentation rewrite complete!${NC}"
    echo -e "${YELLOW}Summary of changes:${NC}"
    git status --short
    
    # Commit changes
    echo -e "\n${YELLOW}Committing changes...${NC}"
    git add .
    git commit -m "docs: Rewrite $extension_name documentation according to guidelines

- Rewrote all documentation per Krista Extension Documentation Guidelines
- Created/updated core documentation files (README, sidebar, configuration, authentication)
- Documented all catalog requests with comprehensive details
- Renamed images to follow naming convention <MD_FILE_NAME>_<IMAGE_NAME>.png
- Integrated images with alt text and captions
- Removed template placeholder files
- Verified code accuracy against source

Jai Bhavani, Jai Shivaji"
    
    # Ask user to review and push
    echo -e "\n${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   Documentation rewrite complete for: $extension_name${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}"
    echo -e "\n${YELLOW}Please review the changes:${NC}"
    echo -e "  cd $WORK_DIR/krista-global-catalog"
    echo -e "  git log -1"
    echo -e "  git diff HEAD~1"
    
    echo -e "\n${YELLOW}Do you want to push this branch? (y/n):${NC}"
    read -p "> " push_confirm
    
    if [ "$push_confirm" = "y" ] || [ "$push_confirm" = "Y" ]; then
        echo -e "${YELLOW}Pushing branch to remote...${NC}"
        git push origin "$docs_branch"
        
        echo -e "\n${GREEN}✓ Branch pushed successfully!${NC}"
        echo -e "\n${YELLOW}Next steps:${NC}"
        echo -e "  1. Go to GitHub/GitLab/Bitbucket"
        echo -e "  2. Create a Pull Request from ${BLUE}$docs_branch${NC} to ${BLUE}$branch${NC}"
        echo -e "  3. Title: 'docs: Rewrite $extension_name documentation'"
        echo -e "  4. Add reviewers and submit"
        
        echo -e "\n${YELLOW}Press Enter when PR is created to continue...${NC}"
        read
    else
        echo -e "${YELLOW}Skipping push. You can push manually later:${NC}"
        echo -e "  git push origin $docs_branch"
    fi
    
    # Return to original directory
    cd ../..
    
    echo -e "${GREEN}✓ Completed processing: $extension_name${NC}\n"
}

# Cleanup function
cleanup() {
    echo -e "\n${YELLOW}Do you want to delete the $WORK_DIR folder? (y/n):${NC}"
    read -p "> " cleanup_confirm
    
    if [ "$cleanup_confirm" = "y" ] || [ "$cleanup_confirm" = "Y" ]; then
        echo -e "${YELLOW}Cleaning up...${NC}"
        rm -rf "$WORK_DIR"
        echo -e "${GREEN}✓ Cleanup complete${NC}"
    else
        echo -e "${YELLOW}Keeping $WORK_DIR folder${NC}"
    fi
}

# Main execution
main() {
    check_prerequisites
    get_release_branches
    select_branch
    
    # Process selected branches
    echo "$SELECTED_BRANCHES" | while read -r branch; do
        if [ -n "$branch" ]; then
            process_extension "$branch"
            
            # If processing all, ask to continue
            if [ "$PROCESS_ALL" = true ]; then
                echo -e "${YELLOW}Continue to next extension? (y/n):${NC}"
                read -p "> " continue_confirm
                if [ "$continue_confirm" != "y" ] && [ "$continue_confirm" != "Y" ]; then
                    echo -e "${BLUE}Stopping batch processing${NC}"
                    break
                fi
            fi
        fi
    done
    
    # Cleanup
    cleanup
    
    echo -e "\n${GREEN}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${GREEN}║   All documentation processing complete!                  ║${NC}"
    echo -e "${GREEN}║   Jai Bhavani, Jai Shivaji                                ║${NC}"
    echo -e "${GREEN}╚════════════════════════════════════════════════════════════╝${NC}\n"
}

# Run main function
main

