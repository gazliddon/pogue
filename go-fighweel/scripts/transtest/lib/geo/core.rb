 $: << File.expand_path(File.dirname(__FILE__))

# Geometry creating DSL

require 'rubygems'
require 'bundler/setup'

require 'docile'
require 'pp'
require 'rmath3d/rmath3d'


require 'mytransit'

include RMath3D

def dbg_log t
    # puts t
end


module Geo

    def merge_verts varray, restart_marker = 0x7fffffff
        index_base = 0

        indicies = []
        verts = []

        varray.each do |vchunk|
            indicies += vchunk[:indicies].map{ |v| v + index_base }
            indicies << restart_marker
            verts = verts + vchunk[:verts]
            index_base += vchunk[:verts].count
        end

        {:indicies => indicies,
         :verts => verts}
    end


    # {{{ Helper funcs
    def to_rad deg
        radians = (deg / 360.0 ) * Math::PI * 2.0
        return radians
    end

    def not_done msg
        dbg_log "NOT DONE: #{msg}"
    end

    # }}}

    # {{{ DSL Instantiation
    def geo(prim_type, &block)
        gb = GeoBuilder.new(prim_type)
        Docile.dsl_eval(gb, &block).build
    end

    def tri_strips(&block)
        geo(:tri_strips, &block)
    end

    def dsl_verts(&block)
        vb = VertBuilder.new
        Docile.dsl_eval(vb , &block).build
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

        def to_floats
            ret = [@pos.to_a.slice(0,3), @uv, @col].map{|a| a.map( &:to_f )}
            ret.flatten
        end
    end
    # }}}

    # {{{ Class to build array of verts
    class VertBuilder
        attr_accessor :verts
        attr_accessor :prim_type

        def initialize
            # set up defaults
            @verts     = []
            @col       = [1,1,1,1]
            @uv        = [0,0]
            @indicies = []
        end

        def vert x,y,z=0
            vert = Vert.new
            vert.pos(x,y,z)
            vert.col( *@col )
            vert.uv( *@uv)
            @verts << vert
        end

        def uv u,v
            @uv=[u,v]
        end

        def color r,g,b,a=1.0
            @col = [r,g,b,a]
        end

        def build
            {:verts     => @verts,
             :indicies  => @indicies}
        end

        def indicies v
            @indicies = v
        end

    end

    # }}}

    # {{{ class GeoBuilder
    class GeoBuilder
        def initialize(prim_type)
            @restart_marker = 0x7fffffff
            @prim_type = prim_type
            @verts = []
            @mat = RMtx4.new.setIdentity
            @mat_stack = []
        end

        # Rudimentary matrix stuff
        def identity
            @mat = RMtx4.new.setIdentity
            dbg_log "matrix after identity #{@mat}"
        end

        def scale x, y, z = 1
            scale_mat = RMtx4.new.scaling( x,y,z  )
            @mat.mul!(scale_mat)
            dbg_log "matrix after scale #{@mat}"
        end

        def transform verts
            verts.map do |v|
                v.transform(@mat)
            end
        end

        def push
            @mat_stack.push(RMtx4.new(@mat))
        end

        def pop
            @mat = @mat_stack.pop
            dbg_log "matrix after pop #{@mat}"
        end

        def rotate deg, x,y,z
            rad = to_rad deg
            rot_mat = RMtx4.new.rotationAxis(RVec3.new(x,y,z), rad )
            @mat.mul!(rot_mat)

            dbg_log "matrix after rotate #{@mat}"
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
            dbg_log "matrix after translate #{@mat}"
        end

        def restart_maker i
            @restart_maker = i
        end

        def build
            merge_verts @verts, @restart_marker
        end

        def verts(&block)
            verts = dsl_verts(&block)
            vert_array = verts[:verts]
            new_vert_array = transform vert_array
            @verts << verts.merge({:verts => new_vert_array })
        end
    end

    # }}}
    
    def run_script file_name, type = :json
        dbg_log "about run script file #{file_name}"
        txt = File.read file_name
        m = Module.new
        m.module_eval txt
        m.module_eval "module_function :main"
        to_transit(type, [m.main])
    end

end

