
typedef int pthread_mutex_t;
typedef unsigned long int pthread_t;
typedef int pthread_attr_t;
extern void pthread_mutex_lock(pthread_mutex_t *lock) ;
extern void pthread_mutex_unlock(pthread_mutex_t *lock) ;
extern int pthread_create(pthread_t *thread_id , pthread_attr_t const   *attr , void *(*func)(void * ) ,
                          void *arg ) ;


struct my_struct {
	int a[2];
} A;

pthread_mutex_t mutex;

void control_function(void *arg) {
    f();
}

int f() {
	pthread_mutex_lock(&mutex);
    A.a[0] = 2;
	pthread_mutex_unlock(&mutex);
}

int main() {
    int *a;
    pthread_t thread;
	pthread_create(&thread, 0, &control_function, 0);
	A.a[0] = 1;
}
