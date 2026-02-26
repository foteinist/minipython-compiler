unknown_function_called_first()

never_defined_function()

result = len([1, 2, 3])
print("Test with print")

function_called_before_declaration()

def function_called_before_declaration():
    print("I am defined now")

def defined_function():
    print("I am defined")

defined_function()

def outer_function():
    inner_function()
    print("Outer function")

def inner_function():
    print("Inner function")

outer_function()

def sample_func(a, b):
    return a + b

sample_func(5,10)