require 'transit'

io = StringIO.new('', 'w+')
writer = Transit::Writer.new(:json, io)
def v2 x,y
    [x, y]
end

def verts v
    return {:vals => v.flatten,
     :num => v.length
    }
end

# Vert is x y u v, all floats
test_map = {:verts => verts( [v2(0,0), v2(1,0), v2(1,1), v2(0,1)] ),
            :uvs   => verts( [v2(0,0), v2(1,0), v2(1,1), v2(0,1)] ) }

writer.write(test_map)

puts io.string
