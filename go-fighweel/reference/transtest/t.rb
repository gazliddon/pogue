require 'transit'
require 'rmath3d/rmath3d'
require 'pp'

include RMath3D

io = StringIO.new('', 'w+')
writer = Transit::Writer.new(:json, io)


def to_v3 v
    RVec3.new(*v)
end

quad_verts = [ [0,0,1],
               [1,0,0],
               [1,1,0],
               [0,1,0], ].map {|vv| to_v3 vv}

pp quad_verts

data = {}

writer.write(data)
puts io.string


