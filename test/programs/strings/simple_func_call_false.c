int f(char *string)
{
    if(string[0] == 'A') return 1;
    return 0;
}

int main() 
{
	char name1[] = "Adam";
	int x = f(name1);
  	
  	if(x == 1) ERROR: return 1;
  	
	return 0;
}


