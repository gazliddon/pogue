require 'transit'

io = StringIO.new('', 'w+')
writer = Transit::Writer.new(:json, io)

# Vert is x y u v, all floats
def vert x,y,u,v
    [x, y, u, v]
end

def verts v
    return {:vals => v.flatten.each {|vv| vv.to_f},
            :num => v.length }
end

quad = {:indicies => [0,3,1,2],
        :verts => verts( [vert(0,0,0,0), 
                          vert(0,0,0,0),
                          vert(1,1,1,1),
                          vert(0,1,0,1)] )}

data = {:models => {:quad => quad}}


writer.write(data)
puts io.string


