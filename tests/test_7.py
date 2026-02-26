def add(x, y):
    return x + y

def add(a, b):
    return a * b

def process(data, options=None):
    print("Processing")

def process(input_data):
    print("Input")

def example_two(a, b):
    return a + b

def example_two(x, y, z=10):
    return x * y + z

def func1(a, b, c):
    return a + b + c

def func1(a, b):
    return a * b

def config(a, b=1, c=2):
    return a + b + c

def config(x, y):
    return x - y

print(add(2, 3))
print(process([1, 2, 3]))
print(example_two(5, 6))
print(func1(1, 2, 3))
print(config(10, 5))