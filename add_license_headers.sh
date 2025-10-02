#!/bin/bash

# MIT License Header Addition Script for NeuroX.AI
# Copyright (c) 2025 Vishwakarma Industries

LICENSE_HEADER="/*
 * MIT License
 *
 * Copyright (c) 2025 Vishwakarma Industries
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the \"Software\"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED \"AS IS\", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 * NeuroX.AI - AI-Powered Camera Application
 * Developed by Vishwakarma Industries
 */

"

echo "Adding MIT License headers to NeuroX.AI source files..."

# Find all Kotlin and Java files that don't already have the license
find app/src/main/java/app/vishwakarma/neuroXcamera -name "*.kt" -o -name "*.java" | while read file; do
    if ! grep -q "MIT License" "$file"; then
        echo "Adding license header to: $file"
        # Create temporary file with license header + original content
        {
            echo "$LICENSE_HEADER"
            cat "$file"
        } > "$file.tmp"
        
        # Replace original file
        mv "$file.tmp" "$file"
    else
        echo "License header already exists in: $file"
    fi
done

echo "License header addition complete!"
echo "All NeuroX.AI source files now include proper MIT License attribution."
