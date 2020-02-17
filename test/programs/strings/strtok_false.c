char *strtok(char * restrict s1,const char * restrict s2);

int main() {
	char str[20] = "New string";
    	char *pch = strtok (str, " ,.");  
	
	pch = strtok(((char *)0), ",");
	if(pch[1] == 'n') 
		ERROR: return 1;

     	return 0;
}


