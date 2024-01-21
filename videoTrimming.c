#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MAX_FILENAME_LENGTH 256

int main() {
  // Get the current working directory
  char cwd[MAX_FILENAME_LENGTH];
  if (getcwd(cwd, sizeof(cwd)) == NULL) {
    perror("getcwd() failed");
    return EXIT_FAILURE;
  }

  // Iterate over all the video files in the current directory
  struct dirent *entry;
  DIR *dir = opendir(cwd);
  if (dir == NULL) {
    perror("opendir() failed");
    return EXIT_FAILURE;
  }
  while ((entry = readdir(dir)) != NULL) {
    // Check if the file is a video file
    if (strstr(entry->d_name, ".mp4") != NULL) {
      // Get the file name without the extension
      char filename[MAX_FILENAME_LENGTH];
      snprintf(filename, sizeof(filename), "%s/%s", cwd, entry->d_name);
      char *filename_without_ext = strtok(filename, ".");

      // Create a new video file with the first 6 seconds removed
      char command[MAX_FILENAME_LENGTH * 2];
      snprintf(command, sizeof(command), "ffmpeg -i %s -ss 00:00:06 -c copy %s-trimmed.mp4", filename, filename_without_ext);
      system(command);
    }
  }
  closedir(dir);

  return EXIT_SUCCESS;
}
