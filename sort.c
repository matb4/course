#include <stdio.h>
#include <string.h>
#include <dirent.h>
#include <sys/stat.h>

void move_file(char *filename, char *folder) {
   char command[100];
   sprintf(command, "mv %s %s", filename, folder);
   system(command);
}

int main() {
   DIR *d;
   struct dirent *dir;
   d = opendir(".");
   if (d) {
       while ((dir = readdir(d)) != NULL) {
           if (dir->d_type == DT_REG) {
               char *extension = strrchr(dir->d_name, '.');
               if (extension) {
                  if (strcmp(extension, ".jpg") == 0 || strcmp(extension, ".png") == 0) {
                      move_file(dir->d_name, "image");
                  } else if (strcmp(extension, ".mp3") == 0 || strcmp(extension, ".wav") == 0) {
                      move_file(dir->d_name, "audio");
                  } else if (strcmp(extension, ".pdf") == 0 || strcmp(extension, ".epub") == 0) {
                      move_file(dir->d_name, "books");
                  } else if (strcmp(extension, ".mp4") == 0 || strcmp(extension, ".mkv") == 0) {
                      move_file(dir->d_name, "media");
                  }
               }
           }
       }
       closedir(d);
   }
   return 0;
}

