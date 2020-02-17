int main() {
  if (f() == 'b') {
    ERROR: return;
  }
  return 0;
}

char f() {
 return 'a';
}
