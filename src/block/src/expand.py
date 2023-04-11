# expand 32bit input to 64bit output
def expand(input: bytes, key: bytes) -> bytes: 
  assert(len(input) == 4)
  assert(len(key) == 2)
  
  key_int = int.from_bytes(key, "big")
  input_int = int.from_bytes(input, "big")
  
  # 2 bit dari input, 1 bit dari key, 1 bit dari (key << i) ^ (input << i)
  # 0 -> 1 bit dari input
  # 1 -> 1 bit dari key
  # 2 -> 1 bit dari (key) ^ (input >> random(32))
  
  OPTIONS = [1, 2, 1, 0, 0, 0, 0, 0, 2, 0, 2, 0, 1, 1, 0, 2, 1, 1, 0, 0, 0, 1, 2, 2, 1, 2, 0, 0, 2, 2, 2, 0, 2, 0, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1, 2, 1, 0, 1, 0, 0, 0, 1, 2, 0, 2, 1, 0, 2, 2, 0, 0, 0, 0, 1]
  
  input_shift_count = 0
  key_shift_count = 0
  
  temp = []
  for i in range(64):
      operation = OPTIONS[i%len(OPTIONS)] # this
      if operation == 0:
          temp.append(str((input_int >> input_shift_count) & 1))
          input_shift_count += 1
      elif operation == 1:
          temp.append(str((key_int >> key_shift_count) & 1))
          key_shift_count += 1
      else:
          rand_num =(int.from_bytes(key, "big")+i) % 32
          to_add = (key_int) ^ (input_int << rand_num)
          temp.append(str(to_add & 1))
  
  res = "".join(temp)
  res = int(res, 2).to_bytes(8, "big")
  
  assert(len(res) == 8)
  
  return res

if __name__ == "__main__":
    input_number = randbits(32)
    key = randbits(16)
    
    a, b = input_number.to_bytes(4, "big"), key.to_bytes(2, "big")
    res = expand(a, b)
    
    print("RES: ", res)