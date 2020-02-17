int main() {
  if (f() == 'a') {
    ERROR: return;
  }
  return 0;
}

char f() {
 return 'a';
}
