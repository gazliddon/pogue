require 'transit'

require 'pp'



# Take geometry and change it to transit
def to_transit type, obj

    ret_str = StringIO.new ""

    ret_array = obj.map do |prim_batch|
        prim_batch.merge( {:verts => prim_batch[:verts].map(&:to_floats).flatten,
                           :num_of_verts => prim_batch[:verts].count } )
    end

    writer = Transit::Writer.new(type, ret_str) # or :json-verbose, :msgpack
    writer.write(ret_array)

    ret_str.string
end


