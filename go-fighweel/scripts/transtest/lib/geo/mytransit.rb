require 'transit'

require 'pp'



# Take geometry and change it to transit
def to_transit type, obj

    ret_str = StringIO.new ""

    writer = Transit::Writer.new(type, ret_str) # or :json-verbose, :msgpack

    writer.write(obj)

    ret_str.string
end


