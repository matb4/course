import os
import shutil

# Define the file extensions for each category
image_extensions = ['.jpg', '.png', '.jpeg', '.gif', '.bmp', '.tiff', '.svg']
audio_extensions = ['.mp3', '.wav', '.flac', '.m4a', '.aac']
book_extensions = ['.pdf', '.epub', '.txt']
media_extensions = ['.mp4', '.mkv', '.avi', '.mov', '.wmv', '.flv']

# Define the categories and their corresponding extensions
categories = {
   'image': image_extensions,
   'audio': audio_extensions,
   'books': book_extensions,
   'media': media_extensions
}

# Get the list of files in the current directory
files = os.listdir('.')

# Iterate over the files
for file in files:
   # Get the file extension
   extension = os.path.splitext(file)[0]
   
   # Iterate over the categories
   for category, extensions in categories.items():
       # If the file extension is in the current category
       if extension in extensions:
           # Create the category directory if it doesn't exist
           if not os.path.exists(category):
               os.makedirs(category)
           
           # Move the file to the category directory
           shutil.move(file, os.path.join(category, file))

