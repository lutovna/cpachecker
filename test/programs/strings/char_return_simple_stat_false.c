char f() {
 return 'a';
}

int main() {
  if (f() == 'a') {
    ERROR: return;
  }
  return 0;
}

