# =======================================================
# MIXED VIOLATIONS TEST SUITE
# Testing multiple broken rules in single statements
# =======================================================

def get_number(x):
    return 100

def get_string():
    return "hello"

# -------------------------------------------------------
# COMBINATION 1: Rule 3 (Args) + Rule 4 (Type Mismatch)
# -------------------------------------------------------
# 1. get_number(1, 2) -> Error: Expects 1 arg, got 2 [Rule 3]
# 2. "Text" + (result is int) -> Error: String + Int [Rule 4]
print "Text" + get_number(1, 2)

print "Text" + get_string()


# -------------------------------------------------------
# COMBINATION 2: Rule 5 (None) + Rule 4 (Type Mismatch)
# -------------------------------------------------------
nothing = None

# 1. Operation with None -> Error [Rule 5]
# 2. String + None -> Error [Rule 4]
print "Result: " + nothing


# -------------------------------------------------------
# COMBINATION 3: Rule 3 (Args) + Rule 6 (Return Type)
# -------------------------------------------------------
# 1. get_string(1) -> Error: Expects 0 args, got 1 [Rule 3]
# 2. 50 + (result is string) -> Error: Int + String [Rule 6/4]
val = 50 + get_string(1)

omg_k="kwnna"
val1 = 50 + omg_k
val2= 50 +get_number(10)

# -------------------------------------------------------
# COMBINATION 4: Rule 1 (Undeclared) + Rule 4 (Type)
# -------------------------------------------------------
# 1. 'unknown_var' is undeclared [Rule 1 - Pass 2]
# 2. "test" + unknown -> Type is unknown, checks usually ignore this 
#    to avoid spam, but Pass 2 will definitely scream.
print "test" + unknown_var


# -------------------------------------------------------
# COMBINATION 5: Rule 7 (Duplicate) + Rule 3 (Args)
# -------------------------------------------------------
def same_func():
    print "A"

# 1. Duplicate definition [Rule 7]
def same_func(a, b):
    print "B"

same_func()


def add(x,y):
   return "hello world"  
print add(2,1) + 2   



print(undefined_variable)

print(k)
k = 5

x = 10
print(x)


#7
def example_two(a, b):
    return a + b

def example_two(x, y, z=10):
    return x * y + z


unknown_function_called_first()

never_defined_function()

result = len([1, 2, 3])
print("Test with print")

function_called_before_declaration()

def function_called_before_declaration():
    print("I am defined now")