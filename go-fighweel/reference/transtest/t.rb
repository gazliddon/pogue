require 'rubygems'
require 'bundler/setup'

require 'docile'
require 'pp'
require 'rmath3d/rmath3d'
include RMath3D

# {{{ Helper funcs
def to_rad deg
    (deg / 360 ) * (Math::PI * 2)
end

def not_done msg
    puts "NOT DONE: #{msg}"
end

# }}}

# {{{ DSL Instantiation
def geo(&block)
    Docile.dsl_eval( GeoBuilder.new, &block).build
end

def dsl_verts(&block)
    Docile.dsl_eval( VertBuilder.new, &block).build
end
# }}}

# {{{ Vert Obj
class Vert
    def initialize
        @col = [1,1,1,1]
        @uv = [0,0]
        @pos = RVec3.new(0,0,0)
    end

    def pos x,y,z=0
        @pos = RVec3.new(x,y,z)
    end

    def col r,g,b,a=1
        @col = [r,g,b,a]
    end

    def uv u,v
        @uv = [u,v]
    end

    def transform! m
        @pos = @pos.transform m
    end
 
    def transform m
        ret = self.clone
        ret.transform! m
        ret
    end

    def to_float
        [@pos.to_a, @uv,@col].map{|a| a.map( &:to_f )}.flatten
    end

end
# }}}

# {{{ Class to build array of verts
class VertBuilder
    def initialize
        # set up defaults
        @verts     = []
        @col       = [1,1,1,1]
        @uv        = [0,0]
        @prim_type = :tri_strip
    end

    def mk_vert x,y,z
        RVec3.new(x,y,z)
    end

    def vert x,y,z=1
        vert = {:vert => mk_vert( x,y,z ),
                :col  => @col,
                :uv   => @uv }

        @verts << vert
    end

    def type _type
        @prim_type = _type
    end

    def uv u,v
        @uv=[u,v]
    end

    def color r,g,b,a=1.0
        @col = [r,g,b,a]
    end

    def build
        {:prim_type => @prim_type,
         :verts     => @verts}
    end

end

# }}}

# {{{ class GeoBuilder
class GeoBuilder
    def initialize(*args)
        @verts = []
        @mat = RMtx4.new.setIdentity
        @mat_stack = []
    end

    # Rudimentary matrix stuff
    def identity
        @mat = RMtx4.new.setIdentity
    end

    def scale x, y, z = 1
        scale_mat = RMtx4.new.scaling( x,y,z  )
        @mat.mul!(scale_mat)
    end

    def transform verts
        verts.map do |v|
            new_vert = v[:vert].transform(@mat)
            v.merge( {:vert => new_vert} )
        end
    end

    def push
        @mat_stack.push @mat
    end

    def pop
        @mat = @mat_stack.pop
    end

    def rotate deg, x,y,z
        rad = to_rad deg
        rot_mat = RMtx4.new.rotationAxis(RVec3.new(x,y,z),rad )
        @mat.mul!(rot_mat)
    end
    
    def rotate_x deg
        rotate deg, 1,0,0
    end

    def rotate_y deg
        rotate deg, 0,1,0
    end
    
    def rotate_z deg
        rotate deg, 0,0,1
    end

    def translate x,y,z=0
        @mat.mul!(RMtx4.new.translation x,y,z)
    end

    def build
        @verts
    end

    def verts(&block)
        verts = dsl_verts(&block)
        vert_array = verts[:verts]
        new_vert_array = transform vert_array
        @verts << verts.merge({:verts => new_vert_array })
    end
end

# }}}

quad = geo do
    amount = 3
    step = 360 / amount
    identity

    (0..amount).each {|i|

        rotate_z step

        push

        scale 0.5, 0.5
        translate 1,1

        verts do
            type :tri_strip

            color(1,0,0)

            uv(0, 0)
            vert(-1, -1)

            uv(1, 0)
            vert(1, -1)

            uv(1, 1)
            vert(1, 1)

            uv(0, 1)
            vert(-1,  1)
        end

        pop
    }

end

pp quad

puts "all done"

